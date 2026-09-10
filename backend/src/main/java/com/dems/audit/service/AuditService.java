package com.dems.audit.service;

import com.dems.audit.model.AuditEvent;
import com.dems.audit.model.AuditLog;
import com.dems.audit.repository.AuditLogRepository;
import com.dems.common.exception.ApiException;
import com.dems.crypto.service.CryptoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final CryptoService cryptoService;

    private final AtomicLong blockCounter = new AtomicLong(-1);

    @Transactional
    public AuditLog createAuditLog(AuditEvent event, UUID userId, String username,
                                   UUID caseId, UUID documentId, UUID evidenceId,
                                   String description, String metadata) {
        Instant now = Instant.now();

        String previousHash = getLastHash();
        long nextBlockNumber = getNextBlockNumber();

        String entryHash = cryptoService.computeAuditEntryHash(
                nextBlockNumber, event.name(), userId, description, metadata, now, previousHash
        );

        String ipAddress = null;
        String userAgent = null;
        try {
            ipAddress = getCurrentIpAddress();
            userAgent = getCurrentUserAgent();
        } catch (Exception ignored) {
        }

        AuditLog auditLog = AuditLog.builder()
                .event(event)
                .userId(userId)
                .username(username)
                .caseId(caseId)
                .documentId(documentId)
                .evidenceId(evidenceId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .description(description)
                .metadata(metadata)
                .previousHash(previousHash)
                .entryHash(entryHash)
                .blockNumber(nextBlockNumber)
                .createdAt(now)
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit log created: event={}, block={}, user={}", event, nextBlockNumber, username);
        return saved;
    }

    @Async
    @Transactional
    public void logAsync(AuditEvent event, UUID userId, String username,
                         UUID caseId, UUID documentId, UUID evidenceId,
                         String description, String metadata) {
        try {
            createAuditLog(event, userId, username, caseId, documentId, evidenceId, description, metadata);
        } catch (Exception e) {
            log.error("Failed to create async audit log for event: {}", event, e);
        }
    }

    public void log(AuditEvent event, UUID userId, String username,
                    UUID caseId, UUID documentId, UUID evidenceId,
                    String description, String metadata) {
        try {
            createAuditLog(event, userId, username, caseId, documentId, evidenceId, description, metadata);
        } catch (Exception e) {
            log.error("Failed to create audit log for event: {}", event, e);
        }
    }

    public void logSimple(AuditEvent event, String description) {
        log(event, null, null, null, null, null, description, null);
    }

    public void logUserEvent(AuditEvent event, UUID userId, String username, String description) {
        log(event, userId, username, null, null, null, description, null);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(UUID userId, UUID caseId, UUID documentId,
                                       AuditEvent event, Instant startDate, Instant endDate,
                                       Pageable pageable) {
        if (userId != null) {
            return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        if (caseId != null) {
            return auditLogRepository.findByCaseIdOrderByCreatedAtDesc(caseId, pageable);
        }
        if (documentId != null) {
            return auditLogRepository.findByDocumentIdOrderByCreatedAtDesc(documentId, pageable);
        }
        if (event != null) {
            return auditLogRepository.findByEventOrderByCreatedAtDesc(event.name(), pageable);
        }
        if (startDate != null && endDate != null) {
            return auditLogRepository.findByDateRange(startDate, endDate, pageable);
        }
        return auditLogRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public AuditChainVerificationResult verifyAuditChain() {
        List<AuditLog> allLogs = auditLogRepository.findAllOrderedByBlock();
        if (allLogs.isEmpty()) {
            return AuditChainVerificationResult.builder()
                    .valid(true)
                    .verifiedCount(0)
                    .message("No audit logs to verify")
                    .build();
        }

        String previousHash = null;
        long expectedBlock = 0L;
        int verifiedCount = 0;
        Long firstInvalidBlock = null;
        String firstInvalidMessage = null;

        for (AuditLog entry : allLogs) {
            if (!entry.getBlockNumber().equals(expectedBlock)) {
                firstInvalidBlock = entry.getBlockNumber();
                firstInvalidMessage = String.format("Block number mismatch: expected %d, got %d", expectedBlock, entry.getBlockNumber());
                break;
            }

            String previous = expectedBlock == 0 ? null : previousHash;
            if (expectedBlock == 0 && entry.getPreviousHash() != null) {
                firstInvalidBlock = entry.getBlockNumber();
                firstInvalidMessage = "Genesis block should have null previous hash";
                break;
            }
            if (expectedBlock > 0 && !entry.getPreviousHash().equals(previousHash)) {
                firstInvalidBlock = entry.getBlockNumber();
                firstInvalidMessage = "Previous hash mismatch";
                break;
            }

            String computedHash = cryptoService.computeAuditEntryHash(
                    entry.getBlockNumber(), entry.getEvent().name(), entry.getUserId(),
                    entry.getDescription(), entry.getMetadata(), entry.getCreatedAt(), previous
            );

            if (!entry.getEntryHash().equals(computedHash)) {
                firstInvalidBlock = entry.getBlockNumber();
                firstInvalidMessage = "Entry hash does not match computed hash - data tampered";
                break;
            }

            previousHash = entry.getEntryHash();
            expectedBlock++;
            verifiedCount++;
        }

        boolean valid = firstInvalidBlock == null;
        return AuditChainVerificationResult.builder()
                .valid(valid)
                .verifiedCount(verifiedCount)
                .totalCount(allLogs.size())
                .firstInvalidBlock(firstInvalidBlock)
                .message(valid
                        ? String.format("Audit chain verified successfully. %d entries checked.", verifiedCount)
                        : "Audit chain integrity violated: " + firstInvalidMessage)
                .build();
    }

    private String getLastHash() {
        Optional<AuditLog> lastEntry = auditLogRepository.findFirstByOrderByBlockNumberDesc();
        return lastEntry.map(AuditLog::getEntryHash).orElse(null);
    }

    private synchronized long getNextBlockNumber() {
        if (blockCounter.get() == -1) {
            Optional<AuditLog> lastEntry = auditLogRepository.findFirstByOrderByBlockNumberDesc();
            if (lastEntry.isPresent()) {
                blockCounter.set(lastEntry.get().getBlockNumber());
            } else {
                blockCounter.set(-1L);
            }
        }
        return blockCounter.incrementAndGet();
    }

    private String getCurrentIpAddress() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isEmpty()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String getCurrentUserAgent() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        return request.getHeader("User-Agent");
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AuditChainVerificationResult {
        private boolean valid;
        private int verifiedCount;
        private int totalCount;
        private Long firstInvalidBlock;
        private String message;
    }
}
