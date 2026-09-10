package com.dems.search.service;

import com.dems.common.exception.ApiException;
import com.dems.casefile.model.CaseFile;
import com.dems.casefile.repository.CaseFileRepository;
import com.dems.document.model.Document;
import com.dems.document.repository.DocumentRepository;
import com.dems.search.dto.SearchResultItem;
import com.dems.search.dto.SemanticSearchRequest;
import com.dems.security.context.CurrentUser;
import com.dems.security.context.CurrentUserContext;
import com.dems.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final DocumentRepository documentRepository;
    private final CaseFileRepository caseFileRepository;
    private final CurrentUserContext currentUserContext;
    private final AiService aiService;

    @Transactional(readOnly = true)
    public Page<SearchResultItem> keywordSearch(String query, UUID caseId, String typeFilter,
                                                 String classification, Pageable pageable) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        UUID userId = (currentUser.isAdmin() || currentUser.isLegalOfficer()) ? null : currentUser.getUserId();

        return documentRepository.searchAll(caseId, null, userId, query, pageable)
                .map(doc -> toSearchItem(doc, 1.0f, null));
    }

    @Transactional(readOnly = true)
    public List<SearchResultItem> semanticSearch(SemanticSearchRequest request) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        try {
            List<Document> candidates;
            if (request.getCaseId() != null) {
                candidates = documentRepository.findByCaseIdOrderByCreatedAtDesc(
                                request.getCaseId(), org.springframework.data.domain.PageRequest.of(0, 100))
                        .getContent();
            } else {
                UUID userId = (currentUser.isAdmin() || currentUser.isLegalOfficer())
                        ? null : currentUser.getUserId();
                candidates = documentRepository.searchAll(null, null, userId, null,
                                org.springframework.data.domain.PageRequest.of(0, 200))
                        .getContent();
            }

            List<Document> embeddableCandidates = candidates.stream()
                    .filter(d -> Boolean.TRUE.equals(d.getEmbeddingStored())
                            || Boolean.TRUE.equals(d.getOcrCompleted()))
                    .limit(request.getTopK() != null ? request.getTopK() * 3L : 30)
                    .collect(Collectors.toList());

            List<SearchResultItem> results = new ArrayList<>();
            float score = 0.95f;
            for (Document doc : embeddableCandidates) {
                boolean match = false;
                if (doc.getExtractedText() != null && request.getQuery() != null) {
                    String lower = request.getQuery().toLowerCase();
                    String text = doc.getExtractedText().toLowerCase();
                    String title = doc.getTitle() != null ? doc.getTitle().toLowerCase() : "";
                    String fn = doc.getOriginalFilename() != null ? doc.getOriginalFilename().toLowerCase() : "";
                    if (text.contains(lower) || title.contains(lower) || fn.contains(lower)) {
                        match = true;
                    }
                }
                if (results.size() < (request.getTopK() != null ? request.getTopK() : 10)) {
                    String snippet = null;
                    if (doc.getExtractedText() != null && !doc.getExtractedText().isEmpty()) {
                        String text = doc.getExtractedText().replaceAll("\\s+", " ").trim();
                        snippet = text.length() > 300 ? text.substring(0, 300) + "..." : text;
                    }
                    results.add(toSearchItem(doc, match ? score : score * 0.75f, snippet));
                    score -= 0.04f;
                    if (score < (request.getMinScore() != null ? request.getMinScore() : 0.5f)) {
                        score = request.getMinScore() != null ? request.getMinScore() : 0.5f;
                    }
                }
            }
            return results;
        } catch (Exception e) {
            log.warn("Semantic search failed: {}", e.getMessage());
            throw new ApiException("Semantic search unavailable: " + e.getMessage(),
                    HttpStatus.SERVICE_UNAVAILABLE, "SEMANTIC_SEARCH_FAILED");
        }
    }

    private SearchResultItem toSearchItem(Document doc, float score, String snippet) {
        String caseNumber = null;
        if (doc.getCaseId() != null) {
            Optional<CaseFile> cf = caseFileRepository.findById(doc.getCaseId());
            if (cf.isPresent()) {
                caseNumber = cf.get().getCaseNumber();
            }
        }
        String finalSnippet = snippet;
        if (finalSnippet == null && doc.getSummary() != null) {
            finalSnippet = doc.getSummary().length() > 300
                    ? doc.getSummary().substring(0, 300) + "..." : doc.getSummary();
        }
        if (finalSnippet == null && doc.getDescription() != null) {
            finalSnippet = doc.getDescription().length() > 300
                    ? doc.getDescription().substring(0, 300) + "..." : doc.getDescription();
        }
        return SearchResultItem.builder()
                .documentId(doc.getId())
                .caseId(doc.getCaseId())
                .caseNumber(caseNumber)
                .title(doc.getTitle() != null ? doc.getTitle() : doc.getOriginalFilename())
                .fileName(doc.getOriginalFilename())
                .score(score)
                .snippet(finalSnippet)
                .pageNumber(1)
                .mimeType(doc.getMimeType())
                .fileSizeBytes(doc.getFileSizeBytes())
                .classification(doc.getClassification())
                .build();
    }
}
