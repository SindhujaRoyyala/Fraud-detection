package com.dems.document.service;

import com.dems.audit.model.AuditEvent;
import com.dems.audit.service.AuditService;
import com.dems.casefile.model.CaseFile;
import com.dems.casefile.repository.CaseFileRepository;
import com.dems.common.exception.ApiException;
import com.dems.common.exception.ResourceNotFoundException;
import com.dems.crypto.service.CryptoService;
import com.dems.document.dto.*;
import com.dems.document.model.Document;
import com.dems.document.model.DocumentStatus;
import com.dems.document.model.DocumentVersion;
import com.dems.document.repository.DocumentRepository;
import com.dems.document.repository.DocumentVersionRepository;
import com.dems.integrity.service.IntegrityService;
import com.dems.security.context.CurrentUser;
import com.dems.security.context.CurrentUserContext;
import com.dems.storage.service.MinioStorageService;
import com.dems.user.model.User;
import com.dems.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final CaseFileRepository caseFileRepository;
    private final UserRepository userRepository;
    private final MinioStorageService storageService;
    private final CryptoService cryptoService;
    private final IntegrityService integrityService;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;

    @Value("${app.document.allowed-extensions:pdf,doc,docx,xls,xlsx,ppt,pptx,txt,png,jpg,jpeg,gif,tiff,bmp,eml,msg,zip,rar,csv,json,xml,html}")
    private Set<String> allowedExtensions;

    @Value("${app.document.max-size-bytes:524288000}")
    private long maxSizeBytes;

    @Transactional
    public UploadResult uploadDocument(MultipartFile file, UUID caseId, String title,
                                        String description, String tags, Boolean sensitive,
                                        String classification) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        if (!currentUser.canModifyCases() && !currentUser.isAdmin() && !currentUser.isInvestigator()) {
            throw new ApiException("You do not have permission to upload documents",
                    HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        validateFile(file);

        if (caseId != null) {
            CaseFile caseFile = caseFileRepository.findById(caseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Case", caseId.toString()));
            authorizeCaseAccess(currentUser, caseFile, true);
        }

        String sha256Hash;
        try (InputStream is = file.getInputStream()) {
            sha256Hash = cryptoService.sha256HashStream(is);
        } catch (Exception e) {
            throw new ApiException("Failed to compute document hash: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR, "HASH_FAILED");
        }

        Optional<Document> existingOpt = integrityService.findDuplicateByHash(sha256Hash);
        if (existingOpt.isPresent()) {
            Document existing = existingOpt.get();
            log.info("Duplicate detected for upload {}: existing document {}",
                    file.getOriginalFilename(), existing.getId());

            auditService.log(AuditEvent.DOCUMENT_UPLOADED,
                    currentUser.getUserId(), currentUser.getUsername(),
                    existing.getCaseId(), existing.getId(), null,
                    String.format("Duplicate upload detected for %s (matches doc %s)",
                            file.getOriginalFilename(), existing.getId()),
                    null);

            return UploadResult.builder()
                    .document(toResponse(existing))
                    .duplicate(true)
                    .duplicateOfDocumentId(existing.getId().toString())
                    .build();
        }

        String storageKey = generateStorageKey(currentUser.getUserId(), sha256Hash, file.getOriginalFilename());

        try (InputStream is = file.getInputStream()) {
            storageService.uploadFile(storageKey, is, file.getSize(), file.getContentType());
        } catch (Exception e) {
            throw new ApiException("Failed to store document: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR, "STORE_FAILED");
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
        Document doc = Document.builder()
                .caseId(caseId)
                .originalFilename(filename)
                .storageKey(storageKey)
                .mimeType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .sha256Hash(sha256Hash)
                .status(DocumentStatus.UPLOADED)
                .title(title != null ? title : filename)
                .description(description)
                .currentVersion(1)
                .ocrCompleted(false)
                .embeddingStored(false)
                .sensitive(sensitive != null && sensitive)
                .tags(tags)
                .classification(classification)
                .uploadedBy(currentUser.getUserId())
                .build();

        Document saved = documentRepository.save(doc);

        DocumentVersion firstVersion = DocumentVersion.builder()
                .documentId(saved.getId())
                .versionNumber(1)
                .originalFilename(filename)
                .storageKey(storageKey)
                .fileSizeBytes(file.getSize())
                .sha256Hash(sha256Hash)
                .changeDescription("Initial upload")
                .mimeType(file.getContentType())
                .createdBy(currentUser.getUserId())
                .build();
        documentVersionRepository.save(firstVersion);

        auditService.log(AuditEvent.DOCUMENT_UPLOADED,
                currentUser.getUserId(), currentUser.getUsername(),
                saved.getCaseId(), saved.getId(), null,
                String.format("Document %s uploaded (%d bytes, %s) - SHA256: %s",
                        filename, file.getSize(), file.getContentType(), sha256Hash),
                null);
        auditService.log(AuditEvent.DOCUMENT_VERSIONED,
                currentUser.getUserId(), currentUser.getUsername(),
                saved.getCaseId(), saved.getId(), null,
                String.format("Document %s v1 created", filename),
                null);

        try {
            triggerAiProcessing(saved.getId());
        } catch (Exception e) {
            log.warn("Could not trigger AI processing for document {}: {}", saved.getId(), e.getMessage());
        }

        return UploadResult.builder()
                .document(toResponse(saved))
                .duplicate(false)
                .build();
    }

    @Transactional
    public UploadResult uploadNewVersion(UUID documentId, MultipartFile file, String changeDescription) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        Document document = getDocumentAndAuthorize(documentId, true);

        validateFile(file);

        String sha256Hash;
        try (InputStream is = file.getInputStream()) {
            sha256Hash = cryptoService.sha256HashStream(is);
        } catch (Exception e) {
            throw new ApiException("Failed to compute hash: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR, "HASH_FAILED");
        }

        if (documentVersionRepository.existsByDocumentIdAndSha256Hash(documentId, sha256Hash)) {
            return UploadResult.builder()
                    .document(toResponse(document))
                    .duplicate(true)
                    .duplicateOfDocumentId(documentId.toString())
                    .build();
        }

        Integer nextVersion = (documentVersionRepository.findMaxVersionNumber(documentId) != null
                ? documentVersionRepository.findMaxVersionNumber(documentId) : 0) + 1;

        String storageKey = generateStorageKey(currentUser.getUserId(), sha256Hash,
                file.getOriginalFilename() + "_v" + nextVersion);

        try (InputStream is = file.getInputStream()) {
            storageService.uploadFile(storageKey, is, file.getSize(), file.getContentType());
        } catch (Exception e) {
            throw new ApiException("Failed to store version: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR, "STORE_FAILED");
        }

        DocumentVersion version = DocumentVersion.builder()
                .documentId(documentId)
                .versionNumber(nextVersion)
                .originalFilename(file.getOriginalFilename())
                .storageKey(storageKey)
                .fileSizeBytes(file.getSize())
                .sha256Hash(sha256Hash)
                .changeDescription(changeDescription != null ? changeDescription : "Version " + nextVersion)
                .mimeType(file.getContentType())
                .createdBy(currentUser.getUserId())
                .build();
        documentVersionRepository.save(version);

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : document.getOriginalFilename();
        document.setOriginalFilename(filename);
        document.setStorageKey(storageKey);
        document.setFileSizeBytes(file.getSize());
        document.setSha256Hash(sha256Hash);
        document.setCurrentVersion(nextVersion);
        document.setStatus(DocumentStatus.UPLOADED);
        document.setMimeType(file.getContentType());
        document.setOcrCompleted(false);
        document.setEmbeddingStored(false);
        Document saved = documentRepository.save(document);

        auditService.log(AuditEvent.DOCUMENT_VERSIONED,
                currentUser.getUserId(), currentUser.getUsername(),
                saved.getCaseId(), saved.getId(), null,
                String.format("Document version v%d created for %s", nextVersion, filename),
                null);

        try {
            triggerAiProcessing(saved.getId());
        } catch (Exception ignored) {
        }

        return UploadResult.builder()
                .document(toResponse(saved))
                .duplicate(false)
                .build();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(UUID documentId) {
        Document doc = getDocumentAndAuthorize(documentId, false);
        updateLastAccessed(doc);
        return toResponse(doc);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadDocument(UUID documentId, Integer version) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        Document doc = getDocumentAndAuthorize(documentId, false);

        String storageKey;
        String filename;
        String mimeType;

        if (version != null) {
            DocumentVersion dv = documentVersionRepository.findByDocumentIdAndVersionNumber(documentId, version)
                    .orElseThrow(() -> new ResourceNotFoundException("DocumentVersion",
                            documentId + ":v" + version));
            storageKey = dv.getStorageKey();
            filename = dv.getOriginalFilename();
            mimeType = dv.getMimeType();
        } else {
            storageKey = doc.getStorageKey();
            filename = doc.getOriginalFilename();
            mimeType = doc.getMimeType();
        }

        InputStream is = storageService.downloadFile(storageKey);
        updateLastAccessed(doc);

        auditService.log(AuditEvent.DOCUMENT_DOWNLOADED,
                currentUser.getUserId(), currentUser.getUsername(),
                doc.getCaseId(), doc.getId(), null,
                String.format("Document %s downloaded (version: %s)",
                        filename, version != null ? version : "current"),
                null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename.replace("\"", "\\\"") + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(is));
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getDocuments(UUID caseId, DocumentStatus status,
                                               UUID uploadedBy, String search, Pageable pageable) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();

        if (caseId != null) {
            CaseFile caseFile = caseFileRepository.findById(caseId)
                    .orElseThrow(() -> new ResourceNotFoundException("Case", caseId.toString()));
            authorizeCaseAccess(currentUser, caseFile, false);
            return documentRepository.searchInCase(caseId, status, search, pageable)
                    .map(this::toResponse);
        }

        UUID scopedUserId = (currentUser.isAdmin() || currentUser.isLegalOfficer()) ? uploadedBy : currentUser.getUserId();
        return documentRepository.searchAll(null, status, scopedUserId, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> getDocumentVersions(UUID documentId) {
        getDocumentAndAuthorize(documentId, false);
        return documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId)
                .stream()
                .map(this::toVersionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IntegrityVerificationResult verifyIntegrity(UUID documentId) {
        getDocumentAndAuthorize(documentId, false);
        CurrentUser currentUser = currentUserContext.getCurrentUser();
        IntegrityVerificationResult result = integrityService.verifyDocumentIntegrity(documentId);

        auditService.log(AuditEvent.DOCUMENT_INTEGRITY_VERIFIED,
                currentUser.getUserId(), currentUser.getUsername(),
                null, documentId, null,
                String.format("Document integrity verified: %s, valid=%s", documentId, result.isValid()),
                null);
        return result;
    }

    @Transactional
    public DocumentResponse updateDocument(UUID documentId, UpdateDocumentRequest request) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        Document doc = getDocumentAndAuthorize(documentId, true);

        if (request.getTitle() != null) {
            doc.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            doc.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            doc.setStatus(request.getStatus());
        }
        if (request.getCaseId() != null) {
            if (!caseFileRepository.existsById(request.getCaseId())) {
                throw new ResourceNotFoundException("Case", request.getCaseId().toString());
            }
            doc.setCaseId(request.getCaseId());
        }
        if (request.getTags() != null) {
            doc.setTags(request.getTags());
        }
        if (request.getClassification() != null) {
            doc.setClassification(request.getClassification());
        }
        if (request.getSensitive() != null) {
            doc.setSensitive(request.getSensitive());
        }

        Document saved = documentRepository.save(doc);

        auditService.log(AuditEvent.CASE_UPDATED,
                currentUser.getUserId(), currentUser.getUsername(),
                saved.getCaseId(), saved.getId(), null,
                String.format("Document %s metadata updated", saved.getId()),
                null);

        return toResponse(saved);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException("File is empty or missing", HttpStatus.BAD_REQUEST, "EMPTY_FILE");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new ApiException("File size exceeds maximum allowed (" + (maxSizeBytes / 1024 / 1024) + " MB)",
                    HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            int dotIdx = originalFilename.lastIndexOf('.');
            if (dotIdx >= 0 && dotIdx < originalFilename.length() - 1) {
                String ext = originalFilename.substring(dotIdx + 1).toLowerCase();
                if (!allowedExtensions.contains(ext)) {
                    throw new ApiException("File extension not allowed: " + ext,
                            HttpStatus.BAD_REQUEST, "EXTENSION_NOT_ALLOWED");
                }
            }
        }
    }

    private Document getDocumentAndAuthorize(UUID documentId, boolean write) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId.toString()));

        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        if (currentUser.isAdmin()) {
            return doc;
        }

        if (doc.getCaseId() != null) {
            Optional<CaseFile> caseOpt = caseFileRepository.findById(doc.getCaseId());
            if (caseOpt.isPresent()) {
                CaseFile cf = caseOpt.get();
                boolean onCase = currentUser.getUserId().equals(cf.getCreatedBy())
                        || currentUser.getUserId().equals(cf.getAssignedInvestigatorId())
                        || currentUser.getUserId().equals(cf.getAssignedLegalOfficerId())
                        || currentUser.isLegalOfficer();
                if (!onCase) {
                    throw new ApiException("You do not have access to documents in this case",
                            HttpStatus.FORBIDDEN, "ACCESS_DENIED");
                }
                return doc;
            }
        }

        if (!currentUser.getUserId().equals(doc.getUploadedBy()) && !currentUser.isLegalOfficer()) {
            throw new ApiException("You do not have access to this document",
                    HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        if (write && !currentUser.getUserId().equals(doc.getUploadedBy()) && !currentUser.canModifyCases()) {
            throw new ApiException("You do not have permission to modify this document",
                    HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        return doc;
    }

    private void authorizeCaseAccess(CurrentUser currentUser, CaseFile caseFile, boolean write) {
        if (currentUser.isAdmin()) {
            return;
        }
        boolean onCase = currentUser.getUserId().equals(caseFile.getCreatedBy())
                || currentUser.getUserId().equals(caseFile.getAssignedInvestigatorId())
                || currentUser.getUserId().equals(caseFile.getAssignedLegalOfficerId())
                || currentUser.isLegalOfficer();
        if (!onCase) {
            throw new ApiException("You do not have access to this case",
                    HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }
        if (write && !currentUser.canModifyCases()
                && !currentUser.getUserId().equals(caseFile.getCreatedBy())
                && !currentUser.getUserId().equals(caseFile.getAssignedInvestigatorId())) {
            throw new ApiException("You do not have write permission on this case",
                    HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }
    }

    private void updateLastAccessed(Document doc) {
        doc.setLastAccessedAt(Instant.now());
        try {
            documentRepository.save(doc);
        } catch (Exception ignored) {
        }
    }

    private void triggerAiProcessing(UUID documentId) {
        documentRepository.findById(documentId).ifPresent(d -> {
            d.setStatus(DocumentStatus.PROCESSING);
            documentRepository.save(d);
        });
    }

    private String generateStorageKey(UUID userId, String hash, String filename) {
        String safeFilename = filename == null ? "file" : filename.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("documents/%s/%s/%s/%s",
                userId,
                Instant.now().toString().substring(0, 7),
                hash.substring(0, 8),
                UUID.randomUUID() + "_" + safeFilename);
    }

    private DocumentResponse toResponse(Document d) {
        String caseNumber = null;
        String uploaderName = null;

        if (d.getCaseId() != null) {
            Optional<CaseFile> cf = caseFileRepository.findById(d.getCaseId());
            if (cf.isPresent()) {
                caseNumber = cf.get().getCaseNumber();
            }
        }
        if (d.getUploadedBy() != null) {
            Optional<User> u = userRepository.findById(d.getUploadedBy());
            if (u.isPresent()) {
                uploaderName = u.get().getFullName() != null ? u.get().getFullName() : u.get().getUsername();
            }
        }

        return DocumentResponse.builder()
                .id(d.getId())
                .caseId(d.getCaseId())
                .caseNumber(caseNumber)
                .originalFilename(d.getOriginalFilename())
                .storageKey(d.getStorageKey())
                .mimeType(d.getMimeType())
                .fileSizeBytes(d.getFileSizeBytes())
                .sha256Hash(d.getSha256Hash())
                .status(d.getStatus())
                .title(d.getTitle())
                .description(d.getDescription())
                .summary(d.getSummary())
                .currentVersion(d.getCurrentVersion())
                .ocrCompleted(d.getOcrCompleted())
                .embeddingStored(d.getEmbeddingStored())
                .sensitive(d.getSensitive())
                .tags(d.getTags())
                .classification(d.getClassification())
                .uploadedBy(d.getUploadedBy())
                .uploadedByName(uploaderName)
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .lastAccessedAt(d.getLastAccessedAt())
                .build();
    }

    private DocumentVersionResponse toVersionResponse(DocumentVersion v) {
        String creatorName = null;
        if (v.getCreatedBy() != null) {
            Optional<User> u = userRepository.findById(v.getCreatedBy());
            if (u.isPresent()) {
                creatorName = u.get().getFullName() != null ? u.get().getFullName() : u.get().getUsername();
            }
        }
        return DocumentVersionResponse.builder()
                .id(v.getId())
                .documentId(v.getDocumentId())
                .versionNumber(v.getVersionNumber())
                .originalFilename(v.getOriginalFilename())
                .fileSizeBytes(v.getFileSizeBytes())
                .sha256Hash(v.getSha256Hash())
                .changeDescription(v.getChangeDescription())
                .mimeType(v.getMimeType())
                .createdBy(v.getCreatedBy())
                .createdByName(creatorName)
                .createdAt(v.getCreatedAt())
                .build();
    }
}
