package com.dems.dashboard.service;

import com.dems.audit.model.AuditLog;
import com.dems.audit.repository.AuditLogRepository;
import com.dems.casefile.model.CaseFile;
import com.dems.casefile.model.CasePriority;
import com.dems.casefile.model.CaseStatus;
import com.dems.casefile.repository.CaseFileRepository;
import com.dems.common.exception.ResourceNotFoundException;
import com.dems.dashboard.dto.*;
import com.dems.dashboard.dto.RelationshipGraph.GraphEdge;
import com.dems.dashboard.dto.RelationshipGraph.GraphNode;
import com.dems.document.model.Document;
import com.dems.document.model.DocumentStatus;
import com.dems.document.repository.DocumentRepository;
import com.dems.evidence.model.Evidence;
import com.dems.evidence.model.EvidenceStatus;
import com.dems.evidence.repository.EvidenceRepository;
import com.dems.security.context.CurrentUser;
import com.dems.security.context.CurrentUserContext;
import com.dems.security.model.Role;
import com.dems.sharing.model.SharedLink;
import com.dems.sharing.repository.SharedLinkRepository;
import com.dems.user.model.User;
import com.dems.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CaseFileRepository caseFileRepository;
    private final DocumentRepository documentRepository;
    private final EvidenceRepository evidenceRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final SharedLinkRepository sharedLinkRepository;
    private final CurrentUserContext currentUserContext;

    @Transactional(readOnly = true)
    public DashboardStats getDashboardStats() {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();
        boolean isAdmin = currentUser.isAdmin();

        List<CaseFile> allCases = caseFileRepository.findAll();
        List<Document> allDocs = documentRepository.findAll();
        List<Evidence> allEvidence = evidenceRepository.findAll();
        List<User> allUsers = userRepository.findAll();

        UUID userId = currentUser.getUserId();
        List<CaseFile> scopedCases = isAdmin || currentUser.isLegalOfficer() ? allCases
                : allCases.stream().filter(c ->
                userId.equals(c.getCreatedBy())
                        || userId.equals(c.getAssignedInvestigatorId())
                        || userId.equals(c.getAssignedLegalOfficerId())
        ).collect(Collectors.toList());

        Set<UUID> scopedCaseIds = scopedCases.stream().map(CaseFile::getId).collect(Collectors.toSet());
        List<Document> scopedDocs = isAdmin || currentUser.isLegalOfficer() ? allDocs
                : allDocs.stream().filter(d ->
                d.getCaseId() == null || scopedCaseIds.contains(d.getCaseId())
                        || userId.equals(d.getUploadedBy())
        ).collect(Collectors.toList());

        long totalCases = scopedCases.size();
        long activeCases = scopedCases.stream().filter(c -> c.getStatus() == CaseStatus.ACTIVE).count();
        long closedCases = scopedCases.stream().filter(c ->
                c.getStatus() == CaseStatus.CLOSED || c.getStatus() == CaseStatus.ARCHIVED).count();

        long totalDocs = scopedDocs.size();
        long docsProcessed = scopedDocs.stream().filter(d -> d.getStatus() == DocumentStatus.PROCESSED).count();
        long docsPending = scopedDocs.stream().filter(d ->
                d.getStatus() == DocumentStatus.UPLOADED || d.getStatus() == DocumentStatus.PROCESSING).count();

        long totalEv = isAdmin ? allEvidence.size()
                : allEvidence.stream().filter(e -> scopedCaseIds.contains(e.getCaseId())).count();
        long evCustody = allEvidence.stream().filter(e ->
                e.getStatus() == EvidenceStatus.IN_CUSTODY || e.getStatus() == EvidenceStatus.COLLECTED).count();
        long evTransferred = allEvidence.stream().filter(e -> e.getStatus() == EvidenceStatus.TRANSFERRED).count();

        long totalUsersCount = isAdmin ? allUsers.size() : 0;
        long activeUsers = isAdmin ? allUsers.stream().filter(u ->
                Boolean.TRUE.equals(u.getActive()) && !Boolean.TRUE.equals(u.getLocked())).count() : 0;

        long allAudit = auditLogRepository.count();
        long totalShares = sharedLinkRepository.count();
        Instant now = Instant.now();
        long activeShares = sharedLinkRepository.findAll().stream()
                .filter(s -> s.getRevokedAt() == null && s.getExpiresAt().isAfter(now)).count();

        return DashboardStats.builder()
                .totalCases(totalCases)
                .activeCases(activeCases)
                .closedCases(closedCases)
                .totalDocuments(totalDocs)
                .documentsProcessed(docsProcessed)
                .documentsPendingProcessing(docsPending)
                .totalEvidence(totalEv)
                .evidenceInCustody(evCustody)
                .evidenceTransferred(evTransferred)
                .totalUsers(totalUsersCount)
                .activeUsers(activeUsers)
                .totalAuditEvents(allAudit)
                .totalShareLinks(totalShares)
                .activeShareLinks(activeShares)
                .casesByStatus(groupCasesByStatus(scopedCases))
                .casesByPriority(groupCasesByPriority(scopedCases))
                .documentsByType(groupDocsByType(scopedDocs))
                .documentsByMonth(groupDocsByMonth(scopedDocs))
                .evidenceByType(groupEvidenceByType(isAdmin ? allEvidence :
                        allEvidence.stream().filter(e -> scopedCaseIds.contains(e.getCaseId()))
                                .collect(Collectors.toList())))
                .activityByHour(getActivityByHour(currentUser.getUserId(), isAdmin))
                .recentActivity(getRecentActivity(scopedCaseIds, currentUser, isAdmin))
                .topCases(getTopCases(scopedCases, scopedDocs.size() > 0 ? scopedDocs : allDocs, allEvidence))
                .build();
    }

    @Transactional(readOnly = true)
    public RelationshipGraph getCaseRelationshipGraph(UUID caseId) {
        currentUserContext.requireAuthenticatedUser();

        CaseFile caseFile = caseFileRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case", caseId.toString()));

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();

        String caseNodeId = "case_" + caseFile.getId();
        nodes.add(GraphNode.builder()
                .id(caseNodeId).type("CASE").label(caseFile.getCaseNumber() + ": " + caseFile.getTitle())
                .data(Map.of("status", caseFile.getStatus(), "priority", caseFile.getPriority()))
                .build());

        if (caseFile.getCreatedBy() != null) {
            userRepository.findById(caseFile.getCreatedBy()).ifPresent(u -> {
                String id = "user_" + u.getId();
                nodes.add(GraphNode.builder().id(id).type("CREATOR")
                        .label(u.getFullName() != null ? u.getFullName() : u.getUsername())
                        .data(Map.of("role", u.getRole())).build());
                edges.add(GraphEdge.builder().id("e_" + id + "_" + caseNodeId)
                        .source(id).target(caseNodeId).label("CREATED").type("CREATED_BY").build());
            });
        }
        if (caseFile.getAssignedInvestigatorId() != null) {
            userRepository.findById(caseFile.getAssignedInvestigatorId()).ifPresent(u -> {
                String id = "inv_" + u.getId();
                nodes.add(GraphNode.builder().id(id).type("INVESTIGATOR")
                        .label(u.getFullName() != null ? u.getFullName() : u.getUsername()).build());
                edges.add(GraphEdge.builder().id("e_inv_" + u.getId())
                        .source(id).target(caseNodeId).label("INVESTIGATES").type("ASSIGNED").build());
            });
        }
        if (caseFile.getAssignedLegalOfficerId() != null) {
            userRepository.findById(caseFile.getAssignedLegalOfficerId()).ifPresent(u -> {
                String id = "lo_" + u.getId();
                nodes.add(GraphNode.builder().id(id).type("LEGAL_OFFICER")
                        .label(u.getFullName() != null ? u.getFullName() : u.getUsername()).build());
                edges.add(GraphEdge.builder().id("e_lo_" + u.getId())
                        .source(id).target(caseNodeId).label("REVIEWS").type("ASSIGNED").build());
            });
        }

        List<Document> docs = documentRepository.findByCaseIdOrderByCreatedAtDesc(caseId, PageRequest.of(0, 50))
                .getContent();
        for (Document d : docs) {
            String dId = "doc_" + d.getId();
            nodes.add(GraphNode.builder().id(dId).type("DOCUMENT")
                    .label(d.getTitle() != null ? d.getTitle() : d.getOriginalFilename())
                    .data(Map.of("mimeType", d.getMimeType() != null ? d.getMimeType() : "unknown",
                            "size", d.getFileSizeBytes(),
                            "status", d.getStatus()))
                    .build());
            edges.add(GraphEdge.builder().id("e_doc_" + d.getId())
                    .source(caseNodeId).target(dId).label("CONTAINS").type("HAS_DOCUMENT").build());
        }

        List<Evidence> evidenceList = evidenceRepository.findByCaseIdOrderByCreatedAtDesc(caseId, PageRequest.of(0, 50))
                .getContent();
        for (Evidence e : evidenceList) {
            String eId = "ev_" + e.getId();
            nodes.add(GraphNode.builder().id(eId).type("EVIDENCE")
                    .label(e.getEvidenceNumber() + ": " + e.getTitle())
                    .data(Map.of("type", e.getEvidenceType() != null ? e.getEvidenceType() : "unknown",
                            "status", e.getStatus()))
                    .build());
            edges.add(GraphEdge.builder().id("e_ev_" + e.getId())
                    .source(caseNodeId).target(eId).label("HAS_EVIDENCE").type("HAS_EVIDENCE").build());

            if (e.getDocumentId() != null) {
                String target = "doc_" + e.getDocumentId();
                edges.add(GraphEdge.builder().id("e_evdoc_" + e.getId())
                        .source(eId).target(target).label("REFERENCES").type("REF_DOCUMENT").build());
            }
            if (e.getCurrentCustodianId() != null) {
                userRepository.findById(e.getCurrentCustodianId()).ifPresent(u -> {
                    String uid = "cust_" + u.getId();
                    boolean exists = nodes.stream().anyMatch(n -> n.getId().equals(uid));
                    if (!exists) {
                        nodes.add(GraphNode.builder().id(uid).type("CUSTODIAN")
                                .label(u.getFullName() != null ? u.getFullName() : u.getUsername()).build());
                    }
                    edges.add(GraphEdge.builder().id("e_cust_" + e.getId())
                            .source(eId).target(uid).label("CUSTODY_WITH").type("CUSTODY").build());
                });
            }
        }

        return RelationshipGraph.builder().nodes(nodes).edges(edges).build();
    }

    private List<Map<String, Object>> groupCasesByStatus(List<CaseFile> cases) {
        Map<CaseStatus, Long> counts = cases.stream().collect(Collectors.groupingBy(CaseFile::getStatus, Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (CaseStatus s : CaseStatus.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("status", s);
            m.put("count", counts.getOrDefault(s, 0L));
            result.add(m);
        }
        return result;
    }

    private List<Map<String, Object>> groupCasesByPriority(List<CaseFile> cases) {
        Map<CasePriority, Long> counts = cases.stream().collect(Collectors.groupingBy(CaseFile::getPriority, Collectors.counting()));
        List<Map<String, Object>> result = new ArrayList<>();
        for (CasePriority p : CasePriority.values()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("priority", p);
            m.put("count", counts.getOrDefault(p, 0L));
            result.add(m);
        }
        return result;
    }

    private List<Map<String, Object>> groupDocsByType(List<Document> docs) {
        Map<String, Long> byExt = new HashMap<>();
        for (Document d : docs) {
            String key = "unknown";
            if (d.getOriginalFilename() != null && d.getOriginalFilename().contains(".")) {
                String ext = d.getOriginalFilename().substring(d.getOriginalFilename().lastIndexOf('.') + 1);
                if (!ext.isEmpty()) key = ext.toLowerCase();
            } else if (d.getMimeType() != null) {
                key = d.getMimeType();
            }
            byExt.merge(key, 1L, Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        byExt.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type", e.getKey());
                    m.put("count", e.getValue());
                    result.add(m);
                });
        return result;
    }

    private List<Map<String, Object>> groupDocsByMonth(List<Document> docs) {
        Map<String, Long> counts = new TreeMap<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            counts.put(ym.toString(), 0L);
        }
        for (Document d : docs) {
            if (d.getCreatedAt() != null) {
                YearMonth ym = YearMonth.from(d.getCreatedAt().atZone(ZoneOffset.UTC));
                if (counts.containsKey(ym.toString())) {
                    counts.merge(ym.toString(), 1L, Long::sum);
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        counts.forEach((k, v) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", k);
            m.put("count", v);
            result.add(m);
        });
        return result;
    }

    private List<Map<String, Object>> groupEvidenceByType(List<Evidence> evidence) {
        Map<String, Long> counts = new HashMap<>();
        for (Evidence e : evidence) {
            String type = e.getEvidenceType() != null ? e.getEvidenceType() : "Uncategorized";
            counts.merge(type, 1L, Long::sum);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        counts.forEach((k, v) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("evidenceType", k);
            m.put("count", v);
            result.add(m);
        });
        return result;
    }

    private List<Map<String, Object>> getActivityByHour(UUID userId, boolean isAdmin) {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<Integer, Long> counts = new HashMap<>();
        for (int i = 0; i < 24; i++) counts.put(i, 0L);

        Instant since = Instant.now().minus(Duration.ofDays(7));
        List<AuditLog> logs = isAdmin
                ? auditLogRepository.findByDateRange(since, Instant.now(), PageRequest.of(0, 5000)).getContent()
                : auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5000)).getContent();
        for (AuditLog l : logs) {
            if (l.getCreatedAt() != null) {
                int h = l.getCreatedAt().atZone(ZoneOffset.UTC).getHour();
                counts.merge(h, 1L, Long::sum);
            }
        }
        for (int i = 0; i < 24; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("hour", i);
            m.put("count", counts.get(i));
            result.add(m);
        }
        return result;
    }

    private List<RecentActivityItem> getRecentActivity(Set<UUID> scopedCaseIds, CurrentUser cu, boolean isAdmin) {
        List<AuditLog> logs = isAdmin
                ? auditLogRepository.findAll(PageRequest.of(0, 20, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))).getContent()
                : auditLogRepository.findByDateRange(Instant.now().minus(Duration.ofDays(30)), Instant.now(),
                PageRequest.of(0, 100)).getContent().stream()
                .filter(l -> (l.getUserId() != null && l.getUserId().equals(cu.getUserId()))
                        || (l.getCaseId() != null && scopedCaseIds.contains(l.getCaseId())))
                .limit(20).collect(Collectors.toList());

        List<RecentActivityItem> result = new ArrayList<>();
        for (AuditLog l : logs) {
            String uname = l.getUsername();
            if (uname == null && l.getUserId() != null) {
                uname = userRepository.findById(l.getUserId()).map(u -> u.getFullName() != null ? u.getFullName() : u.getUsername()).orElse(null);
            }
            String cn = null;
            if (l.getCaseId() != null) {
                cn = caseFileRepository.findById(l.getCaseId()).map(CaseFile::getCaseNumber).orElse(null);
            }
            result.add(RecentActivityItem.builder()
                    .id(l.getId())
                    .type(l.getEvent().name())
                    .title(l.getEvent().name().replace('_', ' '))
                    .description(l.getDescription())
                    .userId(l.getUserId())
                    .userName(uname)
                    .caseId(l.getCaseId())
                    .caseNumber(cn)
                    .documentId(l.getDocumentId())
                    .createdAt(l.getCreatedAt())
                    .build());
        }
        return result;
    }

    private List<CaseSummary> getTopCases(List<CaseFile> cases, List<Document> docs, List<Evidence> evidence) {
        Map<UUID, Long> docCounts = docs.stream().filter(d -> d.getCaseId() != null)
                .collect(Collectors.groupingBy(Document::getCaseId, Collectors.counting()));
        Map<UUID, Long> evCounts = evidence.stream().filter(e -> e.getCaseId() != null)
                .collect(Collectors.groupingBy(Evidence::getCaseId, Collectors.counting()));

        return cases.stream()
                .sorted(Comparator.comparing(CaseFile::getCreatedAt).reversed())
                .limit(5)
                .map(c -> {
                    String creatorName = null;
                    if (c.getCreatedBy() != null) {
                        creatorName = userRepository.findById(c.getCreatedBy())
                                .map(u -> u.getFullName() != null ? u.getFullName() : u.getUsername()).orElse(null);
                    }
                    return CaseSummary.builder()
                            .id(c.getId())
                            .caseNumber(c.getCaseNumber())
                            .title(c.getTitle())
                            .status(c.getStatus().name())
                            .priority(c.getPriority().name())
                            .documentCount(docCounts.getOrDefault(c.getId(), 0L))
                            .evidenceCount(evCounts.getOrDefault(c.getId(), 0L))
                            .createdBy(c.getCreatedBy())
                            .createdByName(creatorName)
                            .createdAt(c.getCreatedAt())
                            .updatedAt(c.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
