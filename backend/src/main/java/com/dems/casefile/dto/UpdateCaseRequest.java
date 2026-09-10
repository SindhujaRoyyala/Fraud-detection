package com.dems.casefile.dto;

import com.dems.casefile.model.CasePriority;
import com.dems.casefile.model.CaseStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCaseRequest {

    @Size(max = 500)
    private String title;

    @Size(max = 5000)
    private String description;

    private CaseStatus status;

    private CasePriority priority;

    @Size(max = 100)
    private String caseType;

    @Size(max = 200)
    private String jurisdiction;

    private String metadata;

    private UUID assignedInvestigatorId;

    private UUID assignedLegalOfficerId;
}
