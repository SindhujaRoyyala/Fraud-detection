package com.dems.evidence.controller;

import com.dems.common.response.ApiResponse;
import com.dems.evidence.dto.*;
import com.dems.evidence.model.EvidenceStatus;
import com.dems.evidence.service.EvidenceService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/evidence")
@RequiredArgsConstructor
@Tag(name = "Evidence", description = "Evidence registry and chain of custody management APIs")
public class EvidenceController {

    private final EvidenceService evidenceService;

    @PostMapping
    @Operation(summary = "Register evidence", description = "Create evidence record in the chain of custody registry")
    public ResponseEntity<ApiResponse<EvidenceResponse>> registerEvidence(
            @Valid @RequestBody RegisterEvidenceRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Evidence registered", evidenceService.registerEvidence(request)));
    }

    @GetMapping
    @Operation(summary = "List evidence", description = "Paginated list of evidence with filters")
    public ResponseEntity<ApiResponse<Page<EvidenceResponse>>> getEvidence(
            @Parameter(description = "Filter by case ID") @RequestParam(required = false) UUID caseId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) EvidenceStatus status,
            @Parameter(description = "Filter by custodian ID") @RequestParam(required = false) UUID custodianId,
            @Parameter(description = "Search by title/number/description") @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(evidenceService.getEvidence(caseId, status, custodianId, search, pageable)));
    }

    @GetMapping("/{evidenceId}")
    @Operation(summary = "Get evidence by ID", description = "Evidence details with current status and custodian")
    public ResponseEntity<ApiResponse<EvidenceResponse>> getEvidenceById(@PathVariable UUID evidenceId) {
        return ResponseEntity.ok(ApiResponse.ok(evidenceService.getEvidenceById(evidenceId)));
    }

    @GetMapping("/{evidenceId}/chain-of-custody")
    @Operation(summary = "Chain of custody", description = "Full transfer history for evidence (hash-chained)")
    public ResponseEntity<ApiResponse<List<EvidenceTransferResponse>>> getChainOfCustody(@PathVariable UUID evidenceId) {
        return ResponseEntity.ok(ApiResponse.ok(evidenceService.getChainOfCustody(evidenceId)));
    }

    @PostMapping("/{evidenceId}/transfer")
    @Operation(summary = "Transfer evidence", description = "Transfer custody to another user (hash-chained)")
    public ResponseEntity<ApiResponse<EvidenceTransferResponse>> transferEvidence(
            @PathVariable UUID evidenceId,
            @Valid @RequestBody EvidenceTransferRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Evidence transferred",
                evidenceService.transferEvidence(evidenceId, request)));
    }

    @GetMapping("/{evidenceId}/qrcode")
    @Operation(summary = "Generate QR code", description = "Generate PNG QR code image for evidence verification")
    public ResponseEntity<byte[]> generateQRCode(@PathVariable UUID evidenceId) {
        return evidenceService.generateEvidenceQRCode(evidenceId);
    }

    @GetMapping("/qr/verify/{qrToken}")
    @Operation(summary = "Verify QR code (public)", description = "Public endpoint to verify evidence by QR token")
    public ResponseEntity<ApiResponse<EvidenceQRVerificationResponse>> verifyQRCode(@PathVariable String qrToken) {
        return ResponseEntity.ok(ApiResponse.ok(evidenceService.verifyQRCodeToken(qrToken)));
    }
}
