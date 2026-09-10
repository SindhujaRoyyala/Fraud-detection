package com.dems.evidence.dto;

import com.dems.evidence.model.EvidenceStatus;
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
public class EvidenceResponse {

    private UUID id;
    private UUID caseId;
    private String caseNumber;
    private UUID documentId;
    private String evidenceNumber;
    private String title;
    private String description;
    private String evidenceType;
    private EvidenceStatus status;
    private String collectionLocation;
    private Instant collectedAt;
    private UUID collectedBy;
    private String collectedByName;
    private UUID currentCustodianId;
    private String currentCustodianName;
    private String chainHash;
    private String qrCodeData;
    private String metadata;
    private String classification;
    private UUID createdBy;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
}
