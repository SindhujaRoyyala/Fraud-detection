package com.dems.evidence.dto;

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
public class EvidenceQRVerificationResponse {

    private boolean valid;
    private String qrToken;
    private UUID evidenceId;
    private String evidenceNumber;
    private String evidenceTitle;
    private UUID currentCustodianId;
    private String currentCustodianName;
    private String chainHash;
    private int transferCount;
    private Instant generatedAt;
    private String message;
}
