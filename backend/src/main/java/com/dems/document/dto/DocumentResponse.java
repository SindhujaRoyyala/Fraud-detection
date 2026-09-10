package com.dems.document.dto;

import com.dems.document.model.DocumentStatus;
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
public class DocumentResponse {

    private UUID id;
    private UUID caseId;
    private String caseNumber;
    private String originalFilename;
    private String storageKey;
    private String mimeType;
    private Long fileSizeBytes;
    private String sha256Hash;
    private DocumentStatus status;
    private String title;
    private String description;
    private String summary;
    private Integer currentVersion;
    private Boolean ocrCompleted;
    private Boolean embeddingStored;
    private Boolean sensitive;
    private String tags;
    private String classification;
    private UUID uploadedBy;
    private String uploadedByName;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastAccessedAt;
}
