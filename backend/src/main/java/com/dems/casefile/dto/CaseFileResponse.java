package com.dems.casefile.dto;

import com.dems.casefile.model.CasePriority;
import com.dems.casefile.model.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseFileResponse {

    private UUID id;
    private String caseNumber;
    private String title;
    private String description;
    private CaseStatus status;
    private CasePriority priority;
    private String caseType;
    private String jurisdiction;
    private String metadata;
    private UUID assignedInvestigatorId;
    private String assignedInvestigatorName;
    private UUID assignedLegalOfficerId;
    private String assignedLegalOfficerName;
    private UUID createdBy;
    private String createdByName;
    private Instant openedAt;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
