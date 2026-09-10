package com.dems.audit;

import com.dems.audit.model.AuditEvent;
import com.dems.audit.model.AuditLog;
import com.dems.audit.repository.AuditLogRepository;
import com.dems.audit.service.AuditService;
import com.dems.crypto.service.CryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditChainTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private CryptoService cryptoService;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService(
                "test-32-byte-aes-256-secret-key!!",
                "test-16-byte-salt"
        );
        auditService = new AuditService(auditLogRepository, cryptoService);
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Genesis block (block 0) has no previous hash")
    void createAuditLog_Genesis_BlockZero() {
        when(auditLogRepository.findFirstByOrderByBlockNumberDesc()).thenReturn(Optional.empty());
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(a -> a.getArgument(0));

        auditService.createAuditLog(AuditEvent.USER_LOGIN, UUID.randomUUID(), "alice",
                null, null, null, "Login", null);

        AuditLog saved = captor.getValue();
        assertEquals(0L, saved.getBlockNumber());
        assertNull(saved.getPreviousHash());
        assertNotNull(saved.getEntryHash());
        assertEquals(64, saved.getEntryHash().length());
    }

    @Test
    @DisplayName("Second block references previous entry hash")
    void createAuditLog_SecondBlock_ChainsToPrevious() {
        UUID userId = UUID.randomUUID();
        Instant t0 = Instant.now().minusSeconds(60);
        AuditLog prev = AuditLog.builder()
                .blockNumber(0L)
                .event(AuditEvent.USER_LOGIN)
                .entryHash("PREVIOUSHASH".repeat(4))
                .userId(userId)
                .createdAt(t0)
                .build();
        when(auditLogRepository.findFirstByOrderByBlockNumberDesc()).thenReturn(Optional.of(prev));
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(captor.capture())).thenAnswer(a -> a.getArgument(0));

        auditService.createAuditLog(AuditEvent.USER_LOGOUT, userId, "alice",
                null, null, null, "Logout", null);

        AuditLog next = captor.getValue();
        assertEquals(1L, next.getBlockNumber());
        assertEquals(prev.getEntryHash(), next.getPreviousHash());
    }

    @Test
    @DisplayName("Verify chain: single genesis block is valid")
    void verifyAuditChain_SingleBlock_Valid() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        String genesisHash = cryptoService.computeAuditEntryHash(
                0L, AuditEvent.USER_LOGIN.name(), userId, "desc", null, now, null
        );
        AuditLog genesis = AuditLog.builder()
                .blockNumber(0L)
                .event(AuditEvent.USER_LOGIN)
                .userId(userId)
                .description("desc")
                .entryHash(genesisHash)
                .previousHash(null)
                .createdAt(now)
                .build();

        when(auditLogRepository.findAllOrderedByBlock()).thenReturn(List.of(genesis));

        AuditService.AuditChainVerificationResult res = auditService.verifyAuditChain();

        assertTrue(res.isValid());
        assertEquals(1, res.getVerifiedCount());
        assertEquals(1, res.getTotalCount());
        assertNull(res.getFirstInvalidBlock());
    }

    @Test
    @DisplayName("Verify chain: 2 valid blocks chain passes")
    void verifyAuditChain_TwoBlocks_Valid() {
        UUID userId = UUID.randomUUID();
        Instant t0 = Instant.now().minusSeconds(10);
        Instant t1 = Instant.now();

        String h0 = cryptoService.computeAuditEntryHash(
                0L, AuditEvent.CASE_CREATED.name(), userId, "d0", "m0", t0, null
        );
        AuditLog b0 = AuditLog.builder()
                .blockNumber(0L).event(AuditEvent.CASE_CREATED).userId(userId)
                .description("d0").metadata("m0").entryHash(h0).previousHash(null).createdAt(t0).build();

        String h1 = cryptoService.computeAuditEntryHash(
                1L, AuditEvent.CASE_UPDATED.name(), userId, "d1", "m1", t1, h0
        );
        AuditLog b1 = AuditLog.builder()
                .blockNumber(1L).event(AuditEvent.CASE_UPDATED).userId(userId)
                .description("d1").metadata("m1").entryHash(h1).previousHash(h0).createdAt(t1).build();

        when(auditLogRepository.findAllOrderedByBlock()).thenReturn(List.of(b0, b1));

        AuditService.AuditChainVerificationResult res = auditService.verifyAuditChain();

        assertTrue(res.isValid());
        assertEquals(2, res.getVerifiedCount());
    }

    @Test
    @DisplayName("Verify chain: tampered entry hash is detected")
    void verifyAuditChain_Tampered_Detected() {
        UUID userId = UUID.randomUUID();
        Instant t0 = Instant.now();

        // original hash for block 0
        String legitHash = cryptoService.computeAuditEntryHash(
                0L, AuditEvent.USER_LOGIN.name(), userId, "login", null, t0, null
        );
        AuditLog tampered = AuditLog.builder()
                .blockNumber(0L).event(AuditEvent.USER_LOGIN).userId(userId)
                .description("LOGIN TAMPERED DESCRIPTION")
                .entryHash(legitHash)
                .previousHash(null)
                .createdAt(t0)
                .build();

        when(auditLogRepository.findAllOrderedByBlock()).thenReturn(List.of(tampered));

        AuditService.AuditChainVerificationResult res = auditService.verifyAuditChain();

        assertFalse(res.isValid());
        assertEquals(0L, res.getFirstInvalidBlock());
        assertTrue(res.getMessage().toLowerCase().contains("hash")
                || res.getMessage().toLowerCase().contains("tampered"));
    }

    @Test
    @DisplayName("Verify chain: broken previous hash link is detected")
    void verifyAuditChain_BrokenPreviousHashLink() {
        UUID userId = UUID.randomUUID();
        Instant t0 = Instant.now().minusSeconds(10);
        Instant t1 = Instant.now();

        String h0 = cryptoService.computeAuditEntryHash(
                0L, AuditEvent.CASE_CREATED.name(), userId, "A", null, t0, null
        );
        AuditLog b0 = AuditLog.builder()
                .blockNumber(0L).event(AuditEvent.CASE_CREATED).userId(userId)
                .description("A").entryHash(h0).previousHash(null).createdAt(t0).build();

        AuditLog b1BadChain = AuditLog.builder()
                .blockNumber(1L).event(AuditEvent.CASE_UPDATED).userId(userId)
                .description("B")
                .previousHash("DEADBEEF".repeat(8))
                .entryHash(cryptoService.computeAuditEntryHash(
                        1L, AuditEvent.CASE_UPDATED.name(), userId, "B", null, t1, "DEADBEEF".repeat(8)))
                .createdAt(t1).build();

        when(auditLogRepository.findAllOrderedByBlock()).thenReturn(List.of(b0, b1BadChain));

        AuditService.AuditChainVerificationResult res = auditService.verifyAuditChain();

        assertFalse(res.isValid());
        assertEquals(1L, res.getFirstInvalidBlock());
    }

    @Test
    @DisplayName("Verify chain: non-sequential block numbers fail")
    void verifyAuditChain_NonSequentialBlocks_Fail() {
        Instant t = Instant.now();
        AuditLog b0 = AuditLog.builder()
                .blockNumber(0L).event(AuditEvent.CASE_CREATED).description("")
                .entryHash(cryptoService.computeAuditEntryHash(0L, "CASE_CREATED", null, "", null, t, null))
                .previousHash(null).createdAt(t).build();
        AuditLog b5 = AuditLog.builder()
                .blockNumber(5L).event(AuditEvent.CASE_UPDATED).description("")
                .entryHash(cryptoService.computeAuditEntryHash(5L, "CASE_UPDATED", null, "", null, t, b0.getEntryHash()))
                .previousHash(b0.getEntryHash()).createdAt(t).build();

        when(auditLogRepository.findAllOrderedByBlock()).thenReturn(List.of(b0, b5));

        AuditService.AuditChainVerificationResult res = auditService.verifyAuditChain();

        assertFalse(res.isValid());
        assertEquals(5L, res.getFirstInvalidBlock());
        assertTrue(res.getMessage().contains("Block number mismatch"));
    }

    @Test
    @DisplayName("Verify chain: empty chain is valid")
    void verifyAuditChain_Empty_Valid() {
        when(auditLogRepository.findAllOrderedByBlock()).thenReturn(List.of());

        AuditService.AuditChainVerificationResult res = auditService.verifyAuditChain();

        assertTrue(res.isValid());
        assertEquals(0, res.getVerifiedCount());
    }

    @Test
    @DisplayName("AuditEvent enum has critical events")
    void auditEvent_ExpectedEvents() {
        assertNotNull(AuditEvent.valueOf("USER_LOGIN"));
        assertNotNull(AuditEvent.valueOf("DOCUMENT_UPLOADED"));
        assertNotNull(AuditEvent.valueOf("DOCUMENT_DOWNLOADED"));
        assertNotNull(AuditEvent.valueOf("EVIDENCE_REGISTERED"));
        assertNotNull(AuditEvent.valueOf("EVIDENCE_TRANSFERRED"));
        assertNotNull(AuditEvent.valueOf("UNAUTHORIZED_ACCESS_ATTEMPT"));
        assertNotNull(AuditEvent.valueOf("AUDIT_CHAIN_VERIFIED"));
        assertNotNull(AuditEvent.valueOf("SECURITY_ALERT_TRIGGERED"));
    }
}
