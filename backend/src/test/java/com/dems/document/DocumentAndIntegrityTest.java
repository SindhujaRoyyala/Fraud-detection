package com.dems.document;

import com.dems.crypto.service.CryptoService;
import com.dems.document.model.Document;
import com.dems.document.model.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DocumentAndIntegrityTest {

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService(
                "test-32-byte-aes-256-secret-key!!",
                "test-16-byte-salt"
        );
    }

    @Test
    @DisplayName("Document: builder works and defaults are applied")
    void document_Builder_Defaults() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        Document d = Document.builder()
                .id(id)
                .originalFilename("contract.pdf")
                .storageKey("docs/123/abc/contract.pdf")
                .fileSizeBytes(2048L)
                .sha256Hash("abc123".repeat(12).substring(0, 64))
                .title("Contract")
                .uploadedBy(UUID.randomUUID())
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(id, d.getId());
        assertEquals("contract.pdf", d.getOriginalFilename());
        assertEquals(DocumentStatus.UPLOADED, d.getStatus());
        assertEquals(1, d.getCurrentVersion());
        assertFalse(d.getOcrCompleted());
        assertFalse(d.getEmbeddingStored());
        assertFalse(d.getSensitive());
    }

    @Test
    @DisplayName("SHA-256: deterministic hash for same input")
    void sha256_DeterministicHash() {
        byte[] data = "Hello, DEMS!".getBytes();

        String h1 = cryptoService.sha256Hash(data);
        String h2 = cryptoService.sha256Hash(data);

        assertNotNull(h1);
        assertEquals(64, h1.length());
        assertEquals(h1, h2);
    }

    @Test
    @DisplayName("SHA-256: different inputs produce different hashes")
    void sha256_DifferentInputs_DifferentHashes() {
        String h1 = cryptoService.sha256Hash("document-version-1");
        String h2 = cryptoService.sha256Hash("document-version-2");

        assertNotEquals(h1, h2);
    }

    @Test
    @DisplayName("SHA-256: stream produces same result as byte array")
    void sha256_StreamMatchesBytes() throws IOException {
        byte[] data = "Streaming integrity test".repeat(100).getBytes();

        String fromBytes = cryptoService.sha256Hash(data);
        String fromStream;
        try (InputStream is = new ByteArrayInputStream(data)) {
            fromStream = cryptoService.sha256HashStream(is);
        }

        assertEquals(fromBytes, fromStream);
    }

    @Test
    @DisplayName("SHA-256: known vector")
    void sha256_KnownVector() {
        String hash = cryptoService.sha256Hash("");

        // SHA-256 of empty string
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
    }

    @Test
    @DisplayName("AES-256-GCM: encrypt and decrypt roundtrip")
    void aes256_EncryptDecrypt_RoundTrip() {
        String plain = "This is a very secret legal document. Case #42.";

        String encrypted = cryptoService.encryptAes256Gcm(plain);

        assertNotNull(encrypted);
        assertNotEquals(plain, encrypted);

        String decrypted = cryptoService.decryptAes256Gcm(encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    @DisplayName("AES-256-GCM: different ciphertext each time (IV random)")
    void aes256_RandomIV_DifferentCiphertexts() {
        String data = "same plaintext";

        String c1 = cryptoService.encryptAes256Gcm(data);
        String c2 = cryptoService.encryptAes256Gcm(data);

        assertNotEquals(c1, c2);
        assertEquals(data, cryptoService.decryptAes256Gcm(c1));
        assertEquals(data, cryptoService.decryptAes256Gcm(c2));
    }

    @Test
    @DisplayName("AES-256-GCM: tampered ciphertext fails to decrypt")
    void aes256_Tampered_Fails() {
        String encrypted = cryptoService.encryptAes256Gcm("hello");
        String tampered = "A" + encrypted.substring(1);

        assertThrows(RuntimeException.class, () -> cryptoService.decryptAes256Gcm(tampered));
    }

    @Test
    @DisplayName("Document status: all expected values")
    void documentStatus_Values() {
        assertNotNull(DocumentStatus.UPLOADED);
        assertNotNull(DocumentStatus.PROCESSING);
        assertNotNull(DocumentStatus.PROCESSED);
        assertNotNull(DocumentStatus.PROCESSING_FAILED);
        assertNotNull(DocumentStatus.ARCHIVED);
    }

    @Test
    @DisplayName("Audit entry hash: deterministic based on parameters")
    void auditEntryHash_Deterministic() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        Long block = 42L;

        String h1 = cryptoService.computeAuditEntryHash(block, "USER_LOGIN", userId, "ok", null, now, null);
        String h2 = cryptoService.computeAuditEntryHash(block, "USER_LOGIN", userId, "ok", null, now, null);

        assertEquals(64, h1.length());
        assertEquals(h1, h2);
    }

    @Test
    @DisplayName("Audit entry hash: any field change changes hash")
    void auditEntryHash_AnyChangeAlters() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        String h1 = cryptoService.computeAuditEntryHash(1L, "A", userId, "d", "{}", now, "PREV");
        String h2 = cryptoService.computeAuditEntryHash(1L, "A", userId, "different", "{}", now, "PREV");
        String h3 = cryptoService.computeAuditEntryHash(1L, "A", userId, "d", "{}", now, "OTHER");

        assertNotEquals(h1, h2);
        assertNotEquals(h1, h3);
    }
}
