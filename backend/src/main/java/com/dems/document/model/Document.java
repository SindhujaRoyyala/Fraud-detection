package com.dems.document.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "documents", schema = "public", indexes = {
        @Index(columnList = "case_id"),
        @Index(columnList = "uploaded_by"),
        @Index(columnList = "sha256_hash"),
        @Index(columnList = "status"),
        @Index(columnList = "mime_type"),
        @Index(columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "case_id", columnDefinition = "uuid")
    private UUID caseId;

    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, unique = true, length = 1000)
    private String storageKey;

    @Column(name = "mime_type", length = 200)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "sha256_hash", nullable = false, length = 64, unique = true)
    private String sha256Hash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "description", length = 5000)
    private String description;

    @Column(name = "extracted_text", columnDefinition = "text")
    private String extractedText;

    @Column(name = "extracted_metadata", columnDefinition = "jsonb")
    private String extractedMetadata;

    @Column(name = "summary", length = 10000)
    private String summary;

    @Column(name = "current_version", nullable = false)
    @Builder.Default
    private Integer currentVersion = 1;

    @Column(name = "ocr_completed", nullable = false)
    @Builder.Default
    private Boolean ocrCompleted = false;

    @Column(name = "embedding_stored", nullable = false)
    @Builder.Default
    private Boolean embeddingStored = false;

    @Column(name = "sensitive", nullable = false)
    @Builder.Default
    private Boolean sensitive = false;

    @Column(name = "document_tags", length = 2000)
    private String tags;

    @Column(name = "classification", length = 100)
    private String classification;

    @CreatedBy
    @Column(name = "uploaded_by", columnDefinition = "uuid", updatable = false)
    private UUID uploadedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;
}
