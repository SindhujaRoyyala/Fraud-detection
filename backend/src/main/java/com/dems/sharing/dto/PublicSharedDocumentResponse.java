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
public class PublicSharedDocumentResponse {

    private UUID documentId;
    private String documentTitle;
    private String originalFilename;
    private String mimeType;
    private Long fileSizeBytes;
    private String description;
    private Integer viewCount;
    private Integer maxViews;
    private Instant expiresAt;
    private boolean canDownload;
    private String sharedByName;
    private String notes;
}
