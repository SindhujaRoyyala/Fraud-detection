package com.dems.evidence.dto;

import jakarta.validation.constraints.NotNull;
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
public class EvidenceTransferRequest {

    @NotNull(message = "Recipient user ID is required")
    private UUID toUserId;

    @Size(max = 2000)
    private String transferReason;

    @Size(max = 1000)
    private String location;

    @Size(max = 255)
    private String witnessName;

    @Size(max = 5000)
    private String notes;
}
