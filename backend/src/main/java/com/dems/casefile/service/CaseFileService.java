package com.dems.casefile.service;

import com.dems.audit.model.AuditEvent;
import com.dems.audit.service.AuditService;
import com.dems.casefile.dto.AssignCaseRequest;
import com.dems.casefile.dto.CaseFileResponse;
import com.dems.casefile.dto.CreateCaseRequest;
import com.dems.casefile.dto.UpdateCaseRequest;
import com.dems.casefile.model.CaseFile;
import com.dems.casefile.model.CasePriority;
import com.dems.casefile.model.CaseStatus;
import com.dems.casefile.repository.CaseFileRepository;
import com.dems.common.exception.ApiException;
import com.dems.common.exception.ResourceNotFoundException;
import com.dems.security.context.CurrentUser;
import com.dems.security.context.CurrentUserContext;
import com.dems.user.model.User;
import com.dems.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class CaseFileService {

    private final CaseFileRepository caseFileRepository;
    private final UserRepository userRepository;
    private final CurrentUserContext currentUserContext;
    private final AuditService auditService;

    private final Map<Integer, AtomicInteger> caseCounters = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public Page<CaseFileResponse> getCases(CaseStatus status, CasePriority priority,
                                            String search, Pageable pageable) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        UUID userId = currentUser.isAdmin() || currentUser.isLegalOfficer() ? null : currentUser.getUserId();
        return caseFileRepository.searchCases(status, priority, userId, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CaseFileResponse getCaseById(UUID caseId) {
        CaseFile caseFile = getCaseAndAuthorize(caseId, false);
        return toResponse(caseFile);
    }

    @Transactional
    public CaseFileResponse createCase(CreateCaseRequest request) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        if (!currentUser.canModifyCases() && !currentUser.isAdmin()) {
            throw new ApiException("You do not have permission to create cases", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        CaseFile caseFile = CaseFile.builder()
                .caseNumber(generateCaseNumber())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : CaseStatus.DRAFT)
                .priority(request.getPriority() != null ? request.getPriority() : CasePriority.MEDIUM)
                .caseType(request.getCaseType())
                .jurisdiction(request.getJurisdiction())
                .metadata(request.getMetadata())
                .openedAt(CaseStatus.ACTIVE.equals(request.getStatus()) ? Instant.now() : null)
                .createdBy(currentUser.getUserId())
                .build();

        CaseFile saved = caseFileRepository.save(caseFile);
        log.info("Case created: {} by user {}", saved.getCaseNumber(), currentUser.getUsername());

        auditService.log(AuditEvent.CASE_CREATED, currentUser.getUserId(), currentUser.getUsername(),
                saved.getId(), null, null,
                String.format("Case %s created with title: %s", saved.getCaseNumber(), saved.getTitle()),
                null);

        return toResponse(saved);
    }

    @Transactional
    public CaseFileResponse updateCase(UUID caseId, UpdateCaseRequest request) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        CaseFile caseFile = getCaseAndAuthorize(caseId, true);

        CaseStatus oldStatus = caseFile.getStatus();

        if (request.getTitle() != null) {
            caseFile.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            caseFile.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            caseFile.setStatus(request.getStatus());
            if (CaseStatus.ACTIVE.equals(request.getStatus()) && caseFile.getOpenedAt() == null) {
                caseFile.setOpenedAt(Instant.now());
            }
            if (CaseStatus.CLOSED.equals(request.getStatus()) || CaseStatus.ARCHIVED.equals(request.getStatus())) {
                caseFile.setClosedAt(Instant.now());
            }
        }
        if (request.getPriority() != null) {
            caseFile.setPriority(request.getPriority());
        }
        if (request.getCaseType() != null) {
            caseFile.setCaseType(request.getCaseType());
        }
        if (request.getJurisdiction() != null) {
            caseFile.setJurisdiction(request.getJurisdiction());
        }
        if (request.getMetadata() != null) {
            caseFile.setMetadata(request.getMetadata());
        }
        if (request.getAssignedInvestigatorId() != null) {
            caseFile.setAssignedInvestigatorId(request.getAssignedInvestigatorId());
        }
        if (request.getAssignedLegalOfficerId() != null) {
            caseFile.setAssignedLegalOfficerId(request.getAssignedLegalOfficerId());
        }

        CaseFile saved = caseFileRepository.save(caseFile);

        auditService.log(AuditEvent.CASE_UPDATED, currentUser.getUserId(), currentUser.getUsername(),
                saved.getId(), null, null,
                String.format("Case %s updated", saved.getCaseNumber()),
                null);

        if (!oldStatus.equals(saved.getStatus())) {
            auditService.log(AuditEvent.CASE_STATUS_CHANGED, currentUser.getUserId(), currentUser.getUsername(),
                    saved.getId(), null, null,
                    String.format("Case %s status changed from %s to %s",
                            saved.getCaseNumber(), oldStatus, saved.getStatus()),
                    null);
        }

        return toResponse(saved);
    }

    @Transactional
    public CaseFileResponse assignCase(UUID caseId, AssignCaseRequest request) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        if (!currentUser.isAdmin() && !currentUser.canModifyCases()) {
            throw new ApiException("You do not have permission to assign cases", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        CaseFile caseFile = getCaseAndAuthorize(caseId, true);

        StringBuilder changes = new StringBuilder();

        if (request.getInvestigatorId() != null) {
            if (!userRepository.existsById(request.getInvestigatorId())) {
                throw new ResourceNotFoundException("Investigator user", request.getInvestigatorId().toString());
            }
            caseFile.setAssignedInvestigatorId(request.getInvestigatorId());
            changes.append("Investigator assigned: ").append(request.getInvestigatorId()).append(". ");
        }

        if (request.getLegalOfficerId() != null) {
            if (!userRepository.existsById(request.getLegalOfficerId())) {
                throw new ResourceNotFoundException("Legal officer user", request.getLegalOfficerId().toString());
            }
            caseFile.setAssignedLegalOfficerId(request.getLegalOfficerId());
            changes.append("Legal officer assigned: ").append(request.getLegalOfficerId());
        }

        CaseFile saved = caseFileRepository.save(caseFile);

        auditService.log(AuditEvent.CASE_ASSIGNED, currentUser.getUserId(), currentUser.getUsername(),
                saved.getId(), null, null,
                String.format("Case %s assigned: %s", saved.getCaseNumber(), changes),
                null);

        return toResponse(saved);
    }

    private CaseFile getCaseAndAuthorize(UUID caseId, boolean write) {
        CaseFile caseFile = caseFileRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case", caseId.toString()));

        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();

        if (currentUser.isAdmin()) {
            return caseFile;
        }

        boolean hasAccess = currentUser.getUserId().equals(caseFile.getCreatedBy())
                || currentUser.getUserId().equals(caseFile.getAssignedInvestigatorId())
                || currentUser.getUserId().equals(caseFile.getAssignedLegalOfficerId())
                || currentUser.isLegalOfficer();

        if (!hasAccess) {
            auditService.log(AuditEvent.UNAUTHORIZED_ACCESS_ATTEMPT,
                    currentUser.getUserId(), currentUser.getUsername(),
                    caseId, null, null,
                    String.format("Unauthorized attempt to %s case %s",
                            write ? "modify" : "view", caseFile.getCaseNumber()),
                    null);
            throw new ApiException("You do not have access to this case", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        if (write && !currentUser.canModifyCases()
                && !currentUser.getUserId().equals(caseFile.getCreatedBy())
                && !currentUser.getUserId().equals(caseFile.getAssignedInvestigatorId())) {
            throw new ApiException("You do not have permission to modify this case", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        return caseFile;
    }

    private String generateCaseNumber() {
        int year = Year.now().getValue();
        AtomicInteger counter = caseCounters.computeIfAbsent(year, k -> {
            String prefix = "CASE-" + year + "-";
            long maxSeq = caseFileRepository.findAll().stream()
                    .map(CaseFile::getCaseNumber)
                    .filter(cn -> cn.startsWith(prefix))
                    .map(cn -> {
                        try {
                            return Integer.parseInt(cn.substring(prefix.length()));
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .max(Integer::compare)
                    .orElse(0);
            return new AtomicInteger((int) maxSeq);
        });

        int seq = counter.incrementAndGet();
        return String.format("CASE-%d-%05d", year, seq);
    }

    private CaseFileResponse toResponse(CaseFile c) {
        String investigatorName = null;
        String legalOfficerName = null;
        String creatorName = null;

        Optional<User> invOpt = c.getAssignedInvestigatorId() != null
                ? userRepository.findById(c.getAssignedInvestigatorId()) : Optional.empty();
        if (invOpt.isPresent()) {
            investigatorName = invOpt.get().getFullName() != null ? invOpt.get().getFullName() : invOpt.get().getUsername();
        }

        Optional<User> legOpt = c.getAssignedLegalOfficerId() != null
                ? userRepository.findById(c.getAssignedLegalOfficerId()) : Optional.empty();
        if (legOpt.isPresent()) {
            legalOfficerName = legOpt.get().getFullName() != null ? legOpt.get().getFullName() : legOpt.get().getUsername();
        }

        Optional<User> creatorOpt = c.getCreatedBy() != null ? userRepository.findById(c.getCreatedBy()) : Optional.empty();
        if (creatorOpt.isPresent()) {
            creatorName = creatorOpt.get().getFullName() != null ? creatorOpt.get().getFullName() : creatorOpt.get().getUsername();
        }

        return CaseFileResponse.builder()
                .id(c.getId())
                .caseNumber(c.getCaseNumber())
                .title(c.getTitle())
                .description(c.getDescription())
                .status(c.getStatus())
                .priority(c.getPriority())
                .caseType(c.getCaseType())
                .jurisdiction(c.getJurisdiction())
                .metadata(c.getMetadata())
                .assignedInvestigatorId(c.getAssignedInvestigatorId())
                .assignedInvestigatorName(investigatorName)
                .assignedLegalOfficerId(c.getAssignedLegalOfficerId())
                .assignedLegalOfficerName(legalOfficerName)
                .createdBy(c.getCreatedBy())
                .createdByName(creatorName)
                .openedAt(c.getOpenedAt())
                .closedAt(c.getClosedAt())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
