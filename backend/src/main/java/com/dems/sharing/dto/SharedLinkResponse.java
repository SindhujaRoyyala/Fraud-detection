package com.dems.sharing.dto;

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
public class SharedLinkResponse {

    private UUID id;
    private String token;
    private String shareUrl;
    private UUID documentId;
    private String documentTitle;
    private UUID caseId;
    private UUID createdBy;
    private String createdByName;
    private String recipientEmail;
    private String recipientName;
    private boolean requiresAccessCode;
    private Integer maxViews;
    private Integer viewCount;
    private Boolean canDownload;
    private Instant expiresAt;
    private Instant revokedAt;
    private boolean isActive;
    private Instant lastAccessedAt;
    private String notes;
    private Instant createdAt;
}
