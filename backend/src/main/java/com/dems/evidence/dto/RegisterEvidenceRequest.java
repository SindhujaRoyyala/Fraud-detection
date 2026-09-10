package com.dems.evidence.dto;

import com.dems.evidence.model.EvidenceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class RegisterEvidenceRequest {

    @NotNull(message = "Case ID is required")
    private UUID caseId;

    private UUID documentId;

    @NotBlank(message = "Title is required")
    @Size(max = 500)
    private String title;

    @Size(max = 5000)
    private String description;

    @Size(max = 100)
    private String evidenceType;

    @Builder.Default
    private EvidenceStatus status = EvidenceStatus.COLLECTED;

    @Size(max = 1000)
    private String collectionLocation;

    private Instant collectedAt;

    private UUID collectedBy;

    private UUID currentCustodianId;

    private String metadata;

    @Size(max = 100)
    private String classification;
}
