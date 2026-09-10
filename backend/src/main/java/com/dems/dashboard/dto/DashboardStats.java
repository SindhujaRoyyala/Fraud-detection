package com.dems.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStats {

    private long totalCases;
    private long activeCases;
    private long closedCases;
    private long totalDocuments;
    private long documentsProcessed;
    private long documentsPendingProcessing;
    private long totalEvidence;
    private long evidenceInCustody;
    private long evidenceTransferred;
    private long totalUsers;
    private long activeUsers;
    private long totalAuditEvents;
    private long totalShareLinks;
    private long activeShareLinks;

    private List<Map<String, Object>> casesByStatus;
    private List<Map<String, Object>> casesByPriority;
    private List<Map<String, Object>> documentsByType;
    private List<Map<String, Object>> documentsByMonth;
    private List<Map<String, Object>> evidenceByType;
    private List<Map<String, Object>> activityByHour;

    private List<RecentActivityItem> recentActivity;
    private List<CaseSummary> topCases;
}
