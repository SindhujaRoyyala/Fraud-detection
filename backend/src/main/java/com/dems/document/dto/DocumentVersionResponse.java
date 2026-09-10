package com.dems.document.dto;

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
public class DocumentVersionResponse {

    private UUID id;
    private UUID documentId;
    private Integer versionNumber;
    private String originalFilename;
    private Long fileSizeBytes;
    private String sha256Hash;
    private String changeDescription;
    private String mimeType;
    private UUID createdBy;
    private String createdByName;
    private Instant createdAt;
}
