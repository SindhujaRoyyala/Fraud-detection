package com.dems.sharing.dto;

import jakarta.validation.constraints.NotNull;
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
public class CreateShareRequest {

    @NotNull(message = "Document ID is required")
    private UUID documentId;

    private UUID caseId;

    private String recipientEmail;

    private String recipientName;

    private String accessCode;

    private Integer expiresHours;

    private Integer maxViews;

    @Builder.Default
    private Boolean canDownload = true;

    private String notes;
}
