package com.dems.search.controller;

import com.dems.common.response.ApiResponse;
import com.dems.search.dto.SearchResultItem;
import com.dems.search.dto.SemanticSearchRequest;
import com.dems.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Keyword and semantic search across documents and cases")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/keyword")
    @Operation(summary = "Keyword search", description = "Full-text keyword search across documents with filters")
    public ResponseEntity<ApiResponse<Page<SearchResultItem>>> keywordSearch(
            @RequestParam String q,
            @RequestParam(required = false) UUID caseId,
            @RequestParam(required = false) String typeFilter,
            @RequestParam(required = false) String classification,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                searchService.keywordSearch(q, caseId, typeFilter, classification, pageable)));
    }

    @PostMapping("/semantic")
    @Operation(summary = "Semantic search (pgvector)", description = "Vector semantic similarity search using BGE-M3 embeddings")
    public ResponseEntity<ApiResponse<List<SearchResultItem>>> semanticSearch(
            @Valid @RequestBody SemanticSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(searchService.semanticSearch(request)));
    }
}
