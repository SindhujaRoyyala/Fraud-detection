package com.dems.integrity.service;

import com.dems.common.exception.ResourceNotFoundException;
import com.dems.crypto.service.CryptoService;
import com.dems.document.dto.IntegrityVerificationResult;
import com.dems.document.model.Document;
import com.dems.document.model.DocumentVersion;
import com.dems.document.repository.DocumentRepository;
import com.dems.document.repository.DocumentVersionRepository;
import com.dems.storage.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrityService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final MinioStorageService storageService;
    private final CryptoService cryptoService;

    @Transactional(readOnly = true)
    public IntegrityVerificationResult verifyDocumentIntegrity(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId.toString()));

        String computedHash;
        try (InputStream is = storageService.downloadFile(document.getStorageKey())) {
            computedHash = cryptoService.sha256HashStream(is);
        } catch (Exception e) {
            log.error("Failed to read document for integrity check: {}", documentId, e);
            return IntegrityVerificationResult.builder()
                    .valid(false)
                    .storedHash(document.getSha256Hash())
                    .computedHash(null)
                    .verifiedAt(Instant.now())
                    .message("Failed to read document from storage for verification: " + e.getMessage())
                    .build();
        }

        boolean valid = document.getSha256Hash().equalsIgnoreCase(computedHash);
        return IntegrityVerificationResult.builder()
                .valid(valid)
                .storedHash(document.getSha256Hash())
                .computedHash(computedHash)
                .verifiedAt(Instant.now())
                .message(valid ? "Document integrity verified - file has not been tampered with"
                        : "WARNING: Document hash mismatch! File may have been tampered with")
                .build();
    }

    @Transactional(readOnly = true)
    public IntegrityVerificationResult verifyDocumentVersionIntegrity(UUID documentId, Integer versionNumber) {
        DocumentVersion version = documentVersionRepository.findByDocumentIdAndVersionNumber(documentId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("DocumentVersion",
                        documentId + ":v" + versionNumber));

        String computedHash;
        try (InputStream is = storageService.downloadFile(version.getStorageKey())) {
            computedHash = cryptoService.sha256HashStream(is);
        } catch (Exception e) {
            log.error("Failed to read version for integrity check: {} v{}", documentId, versionNumber, e);
            return IntegrityVerificationResult.builder()
                    .valid(false)
                    .storedHash(version.getSha256Hash())
                    .computedHash(null)
                    .verifiedAt(Instant.now())
                    .message("Failed to read document version from storage")
                    .build();
        }

        boolean valid = version.getSha256Hash().equalsIgnoreCase(computedHash);
        return IntegrityVerificationResult.builder()
                .valid(valid)
                .storedHash(version.getSha256Hash())
                .computedHash(computedHash)
                .verifiedAt(Instant.now())
                .message(valid ? "Version integrity verified" : "WARNING: Version hash mismatch!")
                .build();
    }

    public Optional<Document> findDuplicateByHash(String sha256Hash) {
        return documentRepository.findBySha256Hash(sha256Hash);
    }
}
