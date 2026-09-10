package com.dems.ai.controller;

import com.dems.ai.dto.AiProcessingResult;
import com.dems.ai.dto.DocumentQaRequest;
import com.dems.ai.dto.DocumentQaResponse;
import com.dems.ai.service.AiService;
import com.dems.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Integration", description = "OCR, summarization, semantic Q&A, metadata extraction APIs")
public class AiController {

    private final AiService aiService;

    @PostMapping("/ocr/{documentId}")
    @Operation(summary = "Run OCR", description = "Run PaddleOCR + PyMuPDF extraction on uploaded document, store text + embeddings")
    public ResponseEntity<ApiResponse<AiProcessingResult>> runOcr(@PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.ok("OCR processing started", aiService.runOcr(documentId)));
    }

    @PostMapping("/metadata/{documentId}")
    @Operation(summary = "Extract metadata", description = "Extract document metadata (author, dates, page count, etc.)")
    public ResponseEntity<ApiResponse<AiProcessingResult>> extractMetadata(@PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.ok(aiService.extractMetadata(documentId)));
    }

    @PostMapping("/summarize/{documentId}")
    @Operation(summary = "Summarize document", description = "LLM-powered document summarization with key phrases")
    public ResponseEntity<ApiResponse<AiProcessingResult>> summarizeDocument(@PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.ok(aiService.summarizeDocument(documentId)));
    }

    @PostMapping("/qa")
    @Operation(summary = "Document Q&A", description = "Ask questions about documents with source & page references (RAG)")
    public ResponseEntity<ApiResponse<DocumentQaResponse>> answerQuestion(
            @Valid @RequestBody DocumentQaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(aiService.answerQuestion(request)));
    }
}
