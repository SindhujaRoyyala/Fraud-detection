package com.dems.sharing.service;

import com.dems.audit.model.AuditEvent;
import com.dems.audit.service.AuditService;
import com.dems.common.exception.ApiException;
import com.dems.common.exception.ResourceNotFoundException;
import com.dems.crypto.service.CryptoService;
import com.dems.document.model.Document;
import com.dems.document.repository.DocumentRepository;
import com.dems.security.context.CurrentUser;
import com.dems.security.context.CurrentUserContext;
import com.dems.security.service.PasswordEncoderService;
import com.dems.sharing.dto.CreateShareRequest;
import com.dems.sharing.dto.PublicSharedDocumentResponse;
import com.dems.sharing.dto.SharedLinkResponse;
import com.dems.sharing.model.SharedLink;
import com.dems.sharing.repository.SharedLinkRepository;
import com.dems.storage.service.MinioStorageService;
import com.dems.user.model.User;
import com.dems.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
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

import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SharingService {

    private final SharedLinkRepository sharedLinkRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final MinioStorageService storageService;
    private final CryptoService cryptoService;
    private final PasswordEncoderService passwordEncoder;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;

    @Value("${app.sharing.default-expiry-hours:72}")
    private int defaultExpiryHours;

    @Value("${app.sharing.max-expiry-hours:720}")
    private int maxExpiryHours;

    @Transactional
    public SharedLinkResponse createShareLink(CreateShareRequest request) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();

        Document doc = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document", request.getDocumentId().toString()));

        if (doc.getCaseId() != null && request.getCaseId() != null
                && !doc.getCaseId().equals(request.getCaseId())) {
            throw new ApiException("Document does not belong to specified case",
                    HttpStatus.BAD_REQUEST, "CASE_MISMATCH");
        }

        int hours = request.getExpiresHours() != null ? request.getExpiresHours() : defaultExpiryHours;
        if (hours > maxExpiryHours) {
            hours = maxExpiryHours;
        }
        if (hours < 1) {
            hours = 1;
        }

        String token = generateUniqueToken();

        String accessCodeHash = null;
        if (request.getAccessCode() != null && !request.getAccessCode().isBlank()) {
            accessCodeHash = passwordEncoder.encode(request.getAccessCode());
        }

        SharedLink link = SharedLink.builder()
                .token(token)
                .documentId(doc.getId())
                .caseId(request.getCaseId() != null ? request.getCaseId() : doc.getCaseId())
                .createdBy(currentUser.getUserId())
                .recipientEmail(request.getRecipientEmail())
                .recipientName(request.getRecipientName())
                .accessCodeHash(accessCodeHash)
                .maxViews(request.getMaxViews())
                .viewCount(0)
                .canDownload(request.getCanDownload() != null ? request.getCanDownload() : true)
                .expiresAt(Instant.now().plus(hours, ChronoUnit.HOURS))
                .notes(request.getNotes())
                .build();

        SharedLink saved = sharedLinkRepository.save(link);

        auditService.log(AuditEvent.SHARING_LINK_CREATED,
                currentUser.getUserId(), currentUser.getUsername(),
                saved.getCaseId(), doc.getId(), null,
                String.format("Share link created for %s (expires: %s, views: %s)",
                        doc.getOriginalFilename(), saved.getExpiresAt(), request.getMaxViews()),
                null);

        return toResponse(saved, doc);
    }

    @Transactional(readOnly = true)
    public Page<SharedLinkResponse> getMyShareLinks(Boolean activeOnly, Pageable pageable) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        if (Boolean.TRUE.equals(activeOnly)) {
            return sharedLinkRepository.findActiveByCreatedBy(currentUser.getUserId(), Instant.now(), pageable)
                    .map(link -> toResponse(link, null));
        }
        return sharedLinkRepository.findByCreatedByOrderByCreatedAtDesc(currentUser.getUserId(), pageable)
                .map(link -> toResponse(link, null));
    }

    @Transactional
    public void revokeShareLink(UUID shareId) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        SharedLink link = sharedLinkRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("ShareLink", shareId.toString()));

        if (!currentUser.isAdmin() && !link.getCreatedBy().equals(currentUser.getUserId())) {
            throw new ApiException("Only the creator can revoke this link",
                    HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        link.setRevokedAt(Instant.now());
        sharedLinkRepository.save(link);

        auditService.log(AuditEvent.SHARING_LINK_CREATED,
                currentUser.getUserId(), currentUser.getUsername(),
                link.getCaseId(), link.getDocumentId(), null,
                "Share link revoked: " + link.getToken(),
                null);
    }

    @Transactional(readOnly = true)
    public PublicSharedDocumentResponse getPublicSharedDocument(String token, String accessCode,
                                                                 HttpServletRequest httpRequest) {
        SharedLink link = validateAndUseLink(token, accessCode, httpRequest);
        Document doc = documentRepository.findById(link.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document", link.getDocumentId().toString()));

        String sharerName = userRepository.findById(link.getCreatedBy())
                .map(u -> u.getFullName() != null ? u.getFullName() : u.getUsername())
                .orElse("DEMS User");

        return PublicSharedDocumentResponse.builder()
                .documentId(doc.getId())
                .documentTitle(doc.getTitle())
                .originalFilename(doc.getOriginalFilename())
                .mimeType(doc.getMimeType())
                .fileSizeBytes(doc.getFileSizeBytes())
                .description(doc.getDescription())
                .viewCount(link.getViewCount())
                .maxViews(link.getMaxViews())
                .expiresAt(link.getExpiresAt())
                .canDownload(Boolean.TRUE.equals(link.getCanDownload()))
                .sharedByName(sharerName)
                .notes(link.getNotes())
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadPublicShared(String token, String accessCode,
                                                          HttpServletRequest httpRequest) {
        SharedLink link = validateAndUseLink(token, accessCode, httpRequest);

        if (!Boolean.TRUE.equals(link.getCanDownload())) {
            throw new ApiException("Download not allowed for this shared document",
                    HttpStatus.FORBIDDEN, "DOWNLOAD_NOT_ALLOWED");
        }

        Document doc = documentRepository.findById(link.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document", link.getDocumentId().toString()));

        InputStream is = storageService.downloadFile(doc.getStorageKey());

        auditService.log(AuditEvent.SHARING_LINK_ACCESSED,
                null, "PUBLIC_SHARE",
                link.getCaseId(), link.getDocumentId(), null,
                "Shared document downloaded: " + doc.getOriginalFilename() + " via token " + token,
                null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(doc.getMimeType() != null
                ? MediaType.parseMediaType(doc.getMimeType()) : MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", doc.getOriginalFilename());

        return ResponseEntity.ok().headers(headers).body(new InputStreamResource(is));
    }

    private SharedLink validateAndUseLink(String token, String accessCode, HttpServletRequest request) {
        SharedLink link = sharedLinkRepository.findByToken(token)
                .orElseThrow(() -> new ApiException("Invalid share link",
                        HttpStatus.NOT_FOUND, "LINK_NOT_FOUND"));

        if (link.getRevokedAt() != null) {
            auditService.log(AuditEvent.SHARING_LINK_EXPIRED,
                    null, "PUBLIC_SHARE",
                    link.getCaseId(), link.getDocumentId(), null,
                    "Attempted access to revoked link " + token,
                    null);
            throw new ApiException("This share link has been revoked",
                    HttpStatus.GONE, "LINK_REVOKED");
        }

        if (link.getExpiresAt().isBefore(Instant.now())) {
            auditService.log(AuditEvent.SHARING_LINK_EXPIRED,
                    null, "PUBLIC_SHARE",
                    link.getCaseId(), link.getDocumentId(), null,
                    "Attempted access to expired link " + token,
                    null);
            throw new ApiException("This share link has expired",
                    HttpStatus.GONE, "LINK_EXPIRED");
        }

        if (link.getAccessCodeHash() != null) {
            if (accessCode == null || accessCode.isBlank()) {
                throw new ApiException("Access code required", HttpStatus.UNAUTHORIZED, "ACCESS_CODE_REQUIRED");
            }
            if (!passwordEncoder.matches(accessCode, link.getAccessCodeHash())) {
                throw new ApiException("Invalid access code", HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_CODE");
            }
        }

        if (link.getMaxViews() != null && link.getViewCount() >= link.getMaxViews()) {
            throw new ApiException("This link has reached its maximum view count",
                    HttpStatus.GONE, "MAX_VIEWS_REACHED");
        }

        link.setViewCount(link.getViewCount() + 1);
        link.setLastAccessedAt(Instant.now());
        sharedLinkRepository.save(link);

        auditService.log(AuditEvent.SHARING_LINK_ACCESSED,
                null, "PUBLIC_SHARE",
                link.getCaseId(), link.getDocumentId(), null,
                String.format("Shared document accessed via %s from IP %s",
                        token, request.getRemoteAddr()),
                null);

        return link;
    }

    private String generateUniqueToken() {
        String token;
        int attempts = 0;
        do {
            token = "shr_" + cryptoService.generateSecureRandomToken().replaceAll("-", "").substring(0, 32);
            attempts++;
            if (attempts > 5) {
                token = "shr_" + UUID.randomUUID().toString().replaceAll("-", "");
                break;
            }
        } while (sharedLinkRepository.existsByToken(token));
        return token;
    }

    private SharedLinkResponse toResponse(SharedLink s, Document doc) {
        String docTitle = null;
        String creatorName = null;

        if (doc != null) {
            docTitle = doc.getTitle() != null ? doc.getTitle() : doc.getOriginalFilename();
        } else {
            docTitle = documentRepository.findById(s.getDocumentId())
                    .map(d -> d.getTitle() != null ? d.getTitle() : d.getOriginalFilename())
                    .orElse(null);
        }

        Optional<User> u = userRepository.findById(s.getCreatedBy());
        if (u.isPresent()) {
            creatorName = u.get().getFullName() != null ? u.get().getFullName() : u.get().getUsername();
        }

        boolean active = s.getRevokedAt() == null && s.getExpiresAt().isAfter(Instant.now())
                && (s.getMaxViews() == null || s.getViewCount() < s.getMaxViews());

        return SharedLinkResponse.builder()
                .id(s.getId())
                .token(s.getToken())
                .shareUrl("/api/sharing/public/" + s.getToken())
                .documentId(s.getDocumentId())
                .documentTitle(docTitle)
                .caseId(s.getCaseId())
                .createdBy(s.getCreatedBy())
                .createdByName(creatorName)
                .recipientEmail(s.getRecipientEmail())
                .recipientName(s.getRecipientName())
                .requiresAccessCode(s.getAccessCodeHash() != null)
                .maxViews(s.getMaxViews())
                .viewCount(s.getViewCount())
                .canDownload(s.getCanDownload())
                .expiresAt(s.getExpiresAt())
                .revokedAt(s.getRevokedAt())
                .isActive(active)
                .lastAccessedAt(s.getLastAccessedAt())
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
