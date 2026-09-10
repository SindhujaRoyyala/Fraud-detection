package com.dems.evidence.service;

import com.dems.audit.model.AuditEvent;
import com.dems.audit.service.AuditService;
import com.dems.casefile.model.CaseFile;
import com.dems.casefile.repository.CaseFileRepository;
import com.dems.common.exception.ApiException;
import com.dems.common.exception.ResourceNotFoundException;
import com.dems.crypto.service.CryptoService;
import com.dems.evidence.dto.*;
import com.dems.evidence.model.Evidence;
import com.dems.evidence.model.EvidenceStatus;
import com.dems.evidence.model.EvidenceTransfer;
import com.dems.evidence.repository.EvidenceRepository;
import com.dems.evidence.repository.EvidenceTransferRepository;
import com.dems.security.context.CurrentUser;
import com.dems.security.context.CurrentUserContext;
import com.dems.user.model.User;
import com.dems.user.repository.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.Year;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceService {

    private final EvidenceRepository evidenceRepository;
    private final EvidenceTransferRepository transferRepository;
    private final CaseFileRepository caseFileRepository;
    private final UserRepository userRepository;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;
    private final CryptoService cryptoService;

    private final Map<Integer, AtomicInteger> evidenceCounters = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> transferCounters = new ConcurrentHashMap<>();

    @Transactional
    public EvidenceResponse registerEvidence(RegisterEvidenceRequest request) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        if (!currentUser.canModifyCases() && !currentUser.isAdmin() && !currentUser.isInvestigator()) {
            throw new ApiException("You do not have permission to register evidence",
                    HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        CaseFile caseFile = caseFileRepository.findById(request.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Case", request.getCaseId().toString()));
        authorizeCaseAccess(currentUser, caseFile, true);

        String evidenceNumber = generateEvidenceNumber();
        UUID custodian = request.getCurrentCustodianId() != null
                ? request.getCurrentCustodianId()
                : currentUser.getUserId();

        String chainSeed = evidenceNumber + "|" + custodian + "|" + Instant.now().toString();
        String chainHash = cryptoService.sha256Hash(chainSeed);
        String qrData = "DEMS-EVIDENCE://" + evidenceNumber + "?v=1&h=" + chainHash.substring(0, 16);

        Evidence evidence = Evidence.builder()
                .caseId(request.getCaseId())
                .documentId(request.getDocumentId())
                .evidenceNumber(evidenceNumber)
                .title(request.getTitle())
                .description(request.getDescription())
                .evidenceType(request.getEvidenceType())
                .status(request.getStatus() != null ? request.getStatus() : EvidenceStatus.COLLECTED)
                .collectionLocation(request.getCollectionLocation())
                .collectedAt(request.getCollectedAt() != null ? request.getCollectedAt() : Instant.now())
                .collectedBy(request.getCollectedBy() != null ? request.getCollectedBy() : currentUser.getUserId())
                .currentCustodianId(custodian)
                .chainHash(chainHash)
                .qrCodeData(qrData)
                .metadata(request.getMetadata())
                .classification(request.getClassification())
                .createdBy(currentUser.getUserId())
                .build();

        Evidence saved = evidenceRepository.save(evidence);

        auditService.log(AuditEvent.EVIDENCE_REGISTERED,
                currentUser.getUserId(), currentUser.getUsername(),
                saved.getCaseId(), null, saved.getId(),
                String.format("Evidence %s registered: %s (type: %s)",
                        saved.getEvidenceNumber(), saved.getTitle(), saved.getEvidenceType()),
                null);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<EvidenceResponse> getEvidence(UUID caseId, EvidenceStatus status,
                                               UUID custodianId, String search, Pageable pageable) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        UUID scopedCustodian = (currentUser.isAdmin() || currentUser.isLegalOfficer())
                ? custodianId
                : (custodianId != null ? custodianId : null);
        return evidenceRepository.searchEvidence(caseId, status, scopedCustodian, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EvidenceResponse getEvidenceById(UUID evidenceId) {
        Evidence e = getEvidenceAndAuthorize(evidenceId, false);
        return toResponse(e);
    }

    @Transactional(readOnly = true)
    public List<EvidenceTransferResponse> getChainOfCustody(UUID evidenceId) {
        getEvidenceAndAuthorize(evidenceId, false);
        List<EvidenceTransfer> transfers = transferRepository.findByEvidenceIdOrderByCreatedAtAsc(evidenceId);
        return transfers.stream().map(this::toTransferResponse).collect(Collectors.toList());
    }

    @Transactional
    public EvidenceTransferResponse transferEvidence(UUID evidenceId, EvidenceTransferRequest request) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        Evidence evidence = getEvidenceAndAuthorize(evidenceId, true);

        if (!Objects.equals(evidence.getCurrentCustodianId(), currentUser.getUserId())
                && !currentUser.isAdmin()) {
            throw new ApiException("Only the current custodian can transfer evidence",
                    HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        if (!userRepository.existsById(request.getToUserId())) {
            throw new ResourceNotFoundException("Recipient user", request.getToUserId().toString());
        }

        int nextSeq = transferCounters.computeIfAbsent(evidenceId, k -> new AtomicInteger(0)).incrementAndGet();
        String transferNumber = evidence.getEvidenceNumber() + "-T" + nextSeq;

        Optional<EvidenceTransfer> lastTransfer = transferRepository.findFirstByEvidenceIdOrderByCreatedAtDesc(evidenceId);
        String previousChainHash = lastTransfer.isPresent()
                ? lastTransfer.get().getTransferHash()
                : evidence.getChainHash();

        String transferData = String.format("%s|%s|%s|%s|%s|%s",
                transferNumber,
                evidence.getCurrentCustodianId(),
                request.getToUserId(),
                request.getTransferReason() != null ? request.getTransferReason() : "",
                previousChainHash,
                Instant.now().toString());
        String transferHash = cryptoService.sha256Hash(transferData);

        EvidenceTransfer transfer = EvidenceTransfer.builder()
                .evidenceId(evidenceId)
                .fromUserId(evidence.getCurrentCustodianId())
                .toUserId(request.getToUserId())
                .transferReason(request.getTransferReason())
                .location(request.getLocation())
                .witnessName(request.getWitnessName())
                .notes(request.getNotes())
                .previousChainHash(previousChainHash)
                .transferHash(transferHash)
                .transferNumber(transferNumber)
                .receivedAt(Instant.now())
                .build();

        EvidenceTransfer savedTransfer = transferRepository.save(transfer);

        evidence.setCurrentCustodianId(request.getToUserId());
        evidence.setChainHash(transferHash);
        evidence.setStatus(EvidenceStatus.TRANSFERRED);
        evidenceRepository.save(evidence);

        auditService.log(AuditEvent.EVIDENCE_TRANSFERRED,
                currentUser.getUserId(), currentUser.getUsername(),
                evidence.getCaseId(), null, evidence.getId(),
                String.format("Evidence %s transferred from %s to %s (transfer %s)",
                        evidence.getEvidenceNumber(),
                        transfer.getFromUserId(),
                        transfer.getToUserId(),
                        transferNumber),
                null);

        return toTransferResponse(savedTransfer);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> generateEvidenceQRCode(UUID evidenceId) {
        Evidence evidence = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence", evidenceId.toString()));

        String qrPayload = evidence.getQrCodeData() != null
                ? evidence.getQrCodeData()
                : "DEMS-EVIDENCE://" + evidence.getEvidenceNumber();

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrPayload, BarcodeFormat.QR_CODE, 350, 350);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentDispositionFormData("attachment",
                    "evidence-" + evidence.getEvidenceNumber() + "-qr.png");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(baos.toByteArray());
        } catch (Exception e) {
            throw new ApiException("Failed to generate QR code: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR, "QR_GENERATION_FAILED");
        }
    }

    @Transactional(readOnly = true)
    public EvidenceQRVerificationResponse verifyQRCodeToken(String qrToken) {
        String evidenceNumber;
        try {
            if (qrToken.startsWith("DEMS-EVIDENCE://")) {
                String rest = qrToken.substring("DEMS-EVIDENCE://".length());
                int qIdx = rest.indexOf('?');
                evidenceNumber = qIdx > 0 ? rest.substring(0, qIdx) : rest;
            } else {
                evidenceNumber = qrToken;
            }
        } catch (Exception e) {
            return EvidenceQRVerificationResponse.builder()
                    .valid(false)
                    .qrToken(qrToken)
                    .generatedAt(Instant.now())
                    .message("Invalid QR code format")
                    .build();
        }

        Optional<Evidence> opt = evidenceRepository.findByEvidenceNumber(evidenceNumber);
        if (opt.isEmpty()) {
            return EvidenceQRVerificationResponse.builder()
                    .valid(false)
                    .qrToken(qrToken)
                    .generatedAt(Instant.now())
                    .message("Evidence not found in registry")
                    .build();
        }

        Evidence evidence = opt.get();
        int transferCount = transferRepository.findByEvidenceIdOrderByCreatedAtAsc(evidence.getId()).size();
        String custodianName = null;
        if (evidence.getCurrentCustodianId() != null) {
            Optional<User> u = userRepository.findById(evidence.getCurrentCustodianId());
            if (u.isPresent()) {
                custodianName = u.get().getFullName() != null ? u.get().getFullName() : u.get().getUsername();
            }
        }

        auditService.log(AuditEvent.EVIDENCE_VERIFIED,
                null, "QR_VERIFICATION",
                evidence.getCaseId(), null, evidence.getId(),
                String.format("Evidence %s verified via QR code scan", evidenceNumber),
                null);

        return EvidenceQRVerificationResponse.builder()
                .valid(true)
                .qrToken(qrToken)
                .evidenceId(evidence.getId())
                .evidenceNumber(evidence.getEvidenceNumber())
                .evidenceTitle(evidence.getTitle())
                .currentCustodianId(evidence.getCurrentCustodianId())
                .currentCustodianName(custodianName)
                .chainHash(evidence.getChainHash())
                .transferCount(transferCount)
                .generatedAt(Instant.now())
                .message("Evidence verified successfully in DEMS registry")
                .build();
    }

    private Evidence getEvidenceAndAuthorize(UUID evidenceId, boolean write) {
        Evidence e = evidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence", evidenceId.toString()));

        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        if (currentUser.isAdmin()) {
            return e;
        }

        Optional<CaseFile> cfOpt = caseFileRepository.findById(e.getCaseId());
        if (cfOpt.isPresent()) {
            CaseFile cf = cfOpt.get();
            boolean onCase = currentUser.getUserId().equals(cf.getCreatedBy())
                    || currentUser.getUserId().equals(cf.getAssignedInvestigatorId())
                    || currentUser.getUserId().equals(cf.getAssignedLegalOfficerId())
                    || currentUser.isLegalOfficer()
                    || currentUser.getUserId().equals(e.getCurrentCustodianId());
            if (!onCase) {
                throw new ApiException("You do not have access to evidence in this case",
                        HttpStatus.FORBIDDEN, "ACCESS_DENIED");
            }
        }

        if (write && !currentUser.canModifyCases()
                && !currentUser.getUserId().equals(e.getCurrentCustodianId())) {
            throw new ApiException("You do not have permission to modify this evidence",
                    HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        return e;
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
                && !currentUser.getUserId().equals(caseFile.getCreatedBy())) {
            throw new ApiException("No write permission on case", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }
    }

    private String generateEvidenceNumber() {
        int year = Year.now().getValue();
        AtomicInteger counter = evidenceCounters.computeIfAbsent(year, k -> new AtomicInteger(0));
        int seq = counter.incrementAndGet();
        return String.format("EVD-%d-%06d", year, seq);
    }

    private EvidenceResponse toResponse(Evidence e) {
        String caseNumber = null;
        String collectedByName = null;
        String custodianName = null;
        String creatorName = null;

        if (e.getCaseId() != null) {
            caseNumber = caseFileRepository.findById(e.getCaseId())
                    .map(CaseFile::getCaseNumber).orElse(null);
        }
        if (e.getCollectedBy() != null) {
            collectedByName = userRepository.findById(e.getCollectedBy())
                    .map(u -> u.getFullName() != null ? u.getFullName() : u.getUsername()).orElse(null);
        }
        if (e.getCurrentCustodianId() != null) {
            custodianName = userRepository.findById(e.getCurrentCustodianId())
                    .map(u -> u.getFullName() != null ? u.getFullName() : u.getUsername()).orElse(null);
        }
        if (e.getCreatedBy() != null) {
            creatorName = userRepository.findById(e.getCreatedBy())
                    .map(u -> u.getFullName() != null ? u.getFullName() : u.getUsername()).orElse(null);
        }

        return EvidenceResponse.builder()
                .id(e.getId())
                .caseId(e.getCaseId())
                .caseNumber(caseNumber)
                .documentId(e.getDocumentId())
                .evidenceNumber(e.getEvidenceNumber())
                .title(e.getTitle())
                .description(e.getDescription())
                .evidenceType(e.getEvidenceType())
                .status(e.getStatus())
                .collectionLocation(e.getCollectionLocation())
                .collectedAt(e.getCollectedAt())
                .collectedBy(e.getCollectedBy())
                .collectedByName(collectedByName)
                .currentCustodianId(e.getCurrentCustodianId())
                .currentCustodianName(custodianName)
                .chainHash(e.getChainHash())
                .qrCodeData(e.getQrCodeData())
                .metadata(e.getMetadata())
                .classification(e.getClassification())
                .createdBy(e.getCreatedBy())
                .createdByName(creatorName)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private EvidenceTransferResponse toTransferResponse(EvidenceTransfer t) {
        String fromName = null;
        String toName = null;
        String evidenceNumber = null;

        if (t.getFromUserId() != null) {
            fromName = userRepository.findById(t.getFromUserId())
                    .map(u -> u.getFullName() != null ? u.getFullName() : u.getUsername()).orElse(null);
        }
        toName = userRepository.findById(t.getToUserId())
                .map(u -> u.getFullName() != null ? u.getFullName() : u.getUsername()).orElse(null);
        evidenceNumber = evidenceRepository.findById(t.getEvidenceId())
                .map(Evidence::getEvidenceNumber).orElse(null);

        return EvidenceTransferResponse.builder()
                .id(t.getId())
                .evidenceId(t.getEvidenceId())
                .evidenceNumber(evidenceNumber)
                .fromUserId(t.getFromUserId())
                .fromUserName(fromName)
                .toUserId(t.getToUserId())
                .toUserName(toName)
                .transferReason(t.getTransferReason())
                .location(t.getLocation())
                .witnessName(t.getWitnessName())
                .notes(t.getNotes())
                .previousChainHash(t.getPreviousChainHash())
                .transferHash(t.getTransferHash())
                .transferNumber(t.getTransferNumber())
                .createdAt(t.getCreatedAt())
                .receivedAt(t.getReceivedAt())
                .build();
    }
}
