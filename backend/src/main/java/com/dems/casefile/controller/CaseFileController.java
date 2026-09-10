package com.dems.casefile.controller;

import com.dems.casefile.dto.AssignCaseRequest;
import com.dems.casefile.dto.CaseFileResponse;
import com.dems.casefile.dto.CreateCaseRequest;
import com.dems.casefile.dto.UpdateCaseRequest;
import com.dems.casefile.model.CasePriority;
import com.dems.casefile.model.CaseStatus;
import com.dems.casefile.service.CaseFileService;
import com.dems.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Tag(name = "Case Management", description = "Case file creation, management, and assignment APIs")
public class CaseFileController {

    private final CaseFileService caseFileService;

    @GetMapping
    @Operation(summary = "List cases", description = "Paginated list of cases with optional filters (scoped by user role)")
    public ResponseEntity<ApiResponse<Page<CaseFileResponse>>> getCases(
            @Parameter(description = "Filter by status") @RequestParam(required = false) CaseStatus status,
            @Parameter(description = "Filter by priority") @RequestParam(required = false) CasePriority priority,
            @Parameter(description = "Search by title/description") @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(caseFileService.getCases(status, priority, search, pageable)));
    }

    @GetMapping("/{caseId}")
    @Operation(summary = "Get case by ID", description = "Full case details (requires case access)")
    public ResponseEntity<ApiResponse<CaseFileResponse>> getCaseById(@PathVariable UUID caseId) {
        return ResponseEntity.ok(ApiResponse.ok(caseFileService.getCaseById(caseId)));
    }

    @PostMapping
    @Operation(summary = "Create case", description = "Create new case file (INVESTIGATOR or ADMIN)")
    public ResponseEntity<ApiResponse<CaseFileResponse>> createCase(
            @Valid @RequestBody CreateCaseRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Case created successfully", caseFileService.createCase(request)));
    }

    @PutMapping("/{caseId}")
    @Operation(summary = "Update case", description = "Update case metadata, status, or assignments")
    public ResponseEntity<ApiResponse<CaseFileResponse>> updateCase(
            @PathVariable UUID caseId,
            @Valid @RequestBody UpdateCaseRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Case updated successfully", caseFileService.updateCase(caseId, request)));
    }

    @PostMapping("/{caseId}/assign")
    @Operation(summary = "Assign case", description = "Assign investigator and/or legal officer to case")
    public ResponseEntity<ApiResponse<CaseFileResponse>> assignCase(
            @PathVariable UUID caseId,
            @Valid @RequestBody AssignCaseRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Case assignment updated", caseFileService.assignCase(caseId, request)));
    }
}
