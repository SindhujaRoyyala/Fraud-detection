package com.dems.ai.service;

import com.dems.ai.dto.AiProcessingResult;
import com.dems.ai.dto.DocumentQaRequest;
import com.dems.ai.dto.DocumentQaResponse;
import com.dems.ai.dto.SourceReference;
import com.dems.audit.model.AuditEvent;
import com.dems.audit.service.AuditService;
import com.dems.common.exception.ApiException;
import com.dems.common.exception.ResourceNotFoundException;
import com.dems.document.model.Document;
import com.dems.document.model.DocumentStatus;
import com.dems.document.repository.DocumentRepository;
import com.dems.security.context.CurrentUser;
import com.dems.security.context.CurrentUserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final WebClient aiWebClient;
    private final DocumentRepository documentRepository;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public AiProcessingResult runOcr(UUID documentId) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId.toString()));

        auditService.log(AuditEvent.AI_PROCESSING_STARTED,
                currentUser.getUserId(), currentUser.getUsername(),
                doc.getCaseId(), documentId, null,
                "OCR processing requested for document " + doc.getOriginalFilename(),
                null);

        doc.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(doc);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("document_id", documentId.toString());
            payload.put("storage_key", doc.getStorageKey());
            payload.put("file_name", doc.getOriginalFilename());
            payload.put("mime_type", doc.getMimeType());
            payload.put("case_id", doc.getCaseId() != null ? doc.getCaseId().toString() : null);

            String response = aiWebClient.post()
                    .uri("/api/ocr/process")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMinutes(5))
                    .block();

            AiProcessingResult result = parseOcrResponse(documentId, response);
            applyOcrResult(doc, result);

            auditService.log(AuditEvent.AI_PROCESSING_COMPLETED,
                    currentUser.getUserId(), currentUser.getUsername(),
                    doc.getCaseId(), documentId, null,
                    "OCR completed for document " + doc.getOriginalFilename(),
                    null);

            return result;
        } catch (WebClientResponseException e) {
            doc.setStatus(DocumentStatus.PROCESSING_FAILED);
            documentRepository.save(doc);
            auditService.log(AuditEvent.AI_PROCESSING_FAILED,
                    currentUser.getUserId(), currentUser.getUsername(),
                    doc.getCaseId(), documentId, null,
                    "OCR failed: HTTP " + e.getStatusCode() + " " + e.getMessage(),
                    null);
            throw new ApiException("AI OCR service error: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY, "AI_OCR_FAILED");
        } catch (Exception e) {
            doc.setStatus(DocumentStatus.PROCESSING_FAILED);
            documentRepository.save(doc);
            return AiProcessingResult.builder()
                    .documentId(documentId)
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .processedAt(Instant.now())
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public AiProcessingResult extractMetadata(UUID documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId.toString()));

        CurrentUser currentUser = currentUserContext.getCurrentUser();

        try {
            Map<String, Object> payload = Map.of(
                    "document_id", documentId.toString(),
                    "storage_key", doc.getStorageKey(),
                    "file_name", doc.getOriginalFilename()
            );
            String response = aiWebClient.post()
                    .uri("/api/metadata/extract")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMinutes(2))
                    .block();

            AiProcessingResult result = parseMetadataResponse(documentId, response);

            auditService.log(AuditEvent.AI_PROCESSING_COMPLETED,
                    currentUser.getUserId(), currentUser.getUsername(),
                    doc.getCaseId(), documentId, null,
                    "Metadata extracted for document " + doc.getOriginalFilename(),
                    null);

            return result;
        } catch (Exception e) {
            log.warn("Metadata extraction failed for {}: {}", documentId, e.getMessage());
            return AiProcessingResult.builder()
                    .documentId(documentId)
                    .status("OFFLINE")
                    .metadata(Map.of("offline", true, "reason", e.getMessage()))
                    .processedAt(Instant.now())
                    .build();
        }
    }

    @Transactional
    public AiProcessingResult summarizeDocument(UUID documentId) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId.toString()));

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("document_id", documentId.toString());
            payload.put("text", doc.getExtractedText() != null
                    ? doc.getExtractedText()
                    : doc.getTitle() + " " + doc.getDescription());
            payload.put("file_name", doc.getOriginalFilename());

            String response = aiWebClient.post()
                    .uri("/api/summarize")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMinutes(3))
                    .block();

            AiProcessingResult result = parseSummarizeResponse(documentId, response);
            if (result.getSummary() != null) {
                doc.setSummary(result.getSummary());
                documentRepository.save(doc);
            }
            return result;
        } catch (Exception e) {
            log.warn("Summarization failed for {}: {}", documentId, e.getMessage());
            return AiProcessingResult.builder()
                    .documentId(documentId)
                    .status("OFFLINE")
                    .summary(getFallbackSummary(doc))
                    .processedAt(Instant.now())
                    .errorMessage("AI service offline - using extraction-based summary")
                    .build();
        }
    }

    @Transactional(readOnly = true)
    public DocumentQaResponse answerQuestion(DocumentQaRequest request) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("question", request.getQuestion());
            payload.put("document_ids", request.getDocumentIds() != null
                    ? request.getDocumentIds().stream().map(UUID::toString).toList()
                    : Collections.emptyList());
            payload.put("case_id", request.getCaseId() != null ? request.getCaseId().toString() : null);
            payload.put("top_k", request.getTopK());
            payload.put("include_sources", request.getIncludeSources());
            payload.put("user_id", currentUser.getUserId().toString());

            String response = aiWebClient.post()
                    .uri("/api/qa/answer")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMinutes(3))
                    .block();

            return parseQaResponse(request.getQuestion(), response);
        } catch (Exception e) {
            log.warn("QA failed for question '{}': {}", request.getQuestion(), e.getMessage());
            return DocumentQaResponse.builder()
                    .question(request.getQuestion())
                    .answer("AI service is currently unavailable. Please try again later.")
                    .model("fallback")
                    .processedAt(Instant.now())
                    .sources(Collections.emptyList())
                    .build();
        }
    }

    private AiProcessingResult parseOcrResponse(UUID documentId, String response) {
        try {
            JsonNode node = objectMapper.readTree(response);
            AiProcessingResult.AiProcessingResultBuilder b = AiProcessingResult.builder()
                    .documentId(documentId)
                    .status(node.path("status").asText("COMPLETED"))
                    .processedAt(Instant.now());

            if (node.has("text")) {
                b.ocrText(node.path("text").asText());
            }
            if (node.has("summary")) {
                b.summary(node.path("summary").asText());
            }
            if (node.has("page_count")) {
                b.pageCount(node.path("page_count").asInt());
            }
            if (node.has("processing_time_ms")) {
                b.processingTimeMs(node.path("processing_time_ms").asLong());
            }
            if (node.has("metadata")) {
                b.metadata(node.path("metadata"));
            }
            if (node.has("key_phrases")) {
                List<String> kp = new ArrayList<>();
                node.path("key_phrases").forEach(n -> kp.add(n.asText()));
                b.keyPhrases(kp);
            }
            b.embeddingsStored(node.path("embeddings_stored").asBoolean(false));
            return b.build();
        } catch (Exception e) {
            return AiProcessingResult.builder()
                    .documentId(documentId)
                    .status("PARSE_ERROR")
                    .processedAt(Instant.now())
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private AiProcessingResult parseMetadataResponse(UUID documentId, String response) {
        try {
            JsonNode node = objectMapper.readTree(response);
            return AiProcessingResult.builder()
                    .documentId(documentId)
                    .status(node.path("status").asText("COMPLETED"))
                    .metadata(node.path("metadata"))
                    .pageCount(node.path("page_count").asInt())
                    .processedAt(Instant.now())
                    .build();
        } catch (Exception e) {
            return AiProcessingResult.builder()
                    .documentId(documentId)
                    .status("PARSE_ERROR")
                    .processedAt(Instant.now())
                    .build();
        }
    }

    private AiProcessingResult parseSummarizeResponse(UUID documentId, String response) {
        try {
            JsonNode node = objectMapper.readTree(response);
            return AiProcessingResult.builder()
                    .documentId(documentId)
                    .status(node.path("status").asText("COMPLETED"))
                    .summary(node.path("summary").asText())
                    .keyPhrases(node.has("key_phrases")
                            ? objectMapper.convertValue(node.path("key_phrases"),
                            objectMapper.getTypeFactory().constructCollectionType(List.class, String.class))
                            : null)
                    .processedAt(Instant.now())
                    .processingTimeMs(node.path("processing_time_ms").asLong())
                    .build();
        } catch (Exception e) {
            return AiProcessingResult.builder()
                    .documentId(documentId)
                    .status("PARSE_ERROR")
                    .processedAt(Instant.now())
                    .build();
        }
    }

    private DocumentQaResponse parseQaResponse(String question, String response) {
        try {
            JsonNode node = objectMapper.readTree(response);
            List<SourceReference> sources = new ArrayList<>();
            if (node.has("sources")) {
                for (JsonNode s : node.path("sources")) {
                    sources.add(SourceReference.builder()
                            .documentId(s.path("document_id").asText() != null && !s.path("document_id").asText().isEmpty()
                                    ? UUID.fromString(s.path("document_id").asText()) : null)
                            .documentTitle(s.path("document_title").asText())
                            .pageNumber(s.path("page_number").asInt())
                            .fileName(s.path("file_name").asText())
                            .similarityScore(s.path("score").floatValue())
                            .snippet(s.path("snippet").asText())
                            .build());
                }
            }
            return DocumentQaResponse.builder()
                    .question(question)
                    .answer(node.path("answer").asText())
                    .model(node.path("model").asText("unknown"))
                    .processedAt(Instant.now())
                    .processingTimeMs(node.path("processing_time_ms").asLong())
                    .sources(sources)
                    .build();
        } catch (Exception e) {
            return DocumentQaResponse.builder()
                    .question(question)
                    .answer("Could not parse AI response.")
                    .processedAt(Instant.now())
                    .sources(Collections.emptyList())
                    .build();
        }
    }

    private void applyOcrResult(Document doc, AiProcessingResult result) {
        if (result.getOcrText() != null && !result.getOcrText().isEmpty()) {
            doc.setExtractedText(result.getOcrText());
            doc.setOcrCompleted(true);
        }
        if (result.getSummary() != null) {
            doc.setSummary(result.getSummary());
        }
        if (result.getMetadata() != null) {
            try {
                doc.setExtractedMetadata(objectMapper.writeValueAsString(result.getMetadata()));
            } catch (Exception ignored) {
            }
        }
        if (Boolean.TRUE.equals(result.getEmbeddingsStored())) {
            doc.setEmbeddingStored(true);
        }
        doc.setStatus(DocumentStatus.PROCESSED);
        documentRepository.save(doc);
    }

    private String getFallbackSummary(Document doc) {
        String base = doc.getDescription() != null ? doc.getDescription()
                : (doc.getTitle() != null ? doc.getTitle() : doc.getOriginalFilename());
        if (doc.getExtractedText() != null && !doc.getExtractedText().isEmpty()) {
            String text = doc.getExtractedText().replaceAll("\\s+", " ").trim();
            if (text.length() > 600) {
                text = text.substring(0, 600) + "...";
            }
            return text;
        }
        return base;
    }
}
