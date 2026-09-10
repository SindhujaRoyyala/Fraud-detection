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
public class EvidenceTransferResponse {

    private UUID id;
    private UUID evidenceId;
    private String evidenceNumber;
    private UUID fromUserId;
    private String fromUserName;
    private UUID toUserId;
    private String toUserName;
    private String transferReason;
    private String location;
    private String witnessName;
    private String notes;
    private String previousChainHash;
    private String transferHash;
    private String transferNumber;
    private Instant createdAt;
    private Instant receivedAt;
}
