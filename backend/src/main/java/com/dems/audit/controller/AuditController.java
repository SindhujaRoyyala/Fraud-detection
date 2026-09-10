package com.dems.audit.controller;

import com.dems.audit.model.AuditEvent;
import com.dems.audit.model.AuditLog;
import com.dems.audit.service.AuditService;
import com.dems.common.response.ApiResponse;
import com.dems.common.exception.ApiException;
import com.dems.security.context.CurrentUser;
import com.dems.security.context.CurrentUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Tamper-evident audit log query and chain verification APIs")
public class AuditController {

    private final AuditService auditService;
    private final CurrentUserContext currentUserContext;

    @GetMapping("/logs")
    @Operation(summary = "Query audit logs", description = "Paginated audit log search with filters (ADMIN sees all, others see own)")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @Parameter(description = "Filter by user ID") @RequestParam(required = false) UUID userId,
            @Parameter(description = "Filter by case ID") @RequestParam(required = false) UUID caseId,
            @Parameter(description = "Filter by document ID") @RequestParam(required = false) UUID documentId,
            @Parameter(description = "Filter by event type") @RequestParam(required = false) AuditEvent event,
            @Parameter(description = "Start date (ISO)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End date (ISO)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {

        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        UUID scopedUserId;
        if (currentUser.isAdmin()) {
            scopedUserId = userId;
        } else {
            if (userId != null && !userId.equals(currentUser.getUserId())) {
                throw new ApiException("You can only view your own audit logs",
                        HttpStatus.FORBIDDEN, "ACCESS_DENIED");
            }
            scopedUserId = currentUser.getUserId();
        }

        return ResponseEntity.ok(ApiResponse.ok(
                auditService.getAuditLogs(scopedUserId, caseId, documentId, event, startDate, endDate, pageable)));
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify audit chain", description = "Verify hash chain integrity of ALL audit log entries")
    public ResponseEntity<ApiResponse<AuditService.AuditChainVerificationResult>> verifyAuditChain() {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        if (!currentUser.isAdmin() && !currentUser.isLegalOfficer()) {
            throw new ApiException("Only admins and legal officers can verify the audit chain",
                    HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }
        AuditService.AuditChainVerificationResult result = auditService.verifyAuditChain();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
