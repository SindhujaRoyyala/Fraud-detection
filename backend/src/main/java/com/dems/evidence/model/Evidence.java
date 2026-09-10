package com.dems.evidence.model;

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
@Table(name = "evidence", schema = "public", indexes = {
        @Index(columnList = "case_id"),
        @Index(columnList = "evidence_number", unique = true),
        @Index(columnList = "status"),
        @Index(columnList = "document_id"),
        @Index(columnList = "current_custodian_id"),
        @Index(columnList = "collected_by"),
        @Index(columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Evidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "case_id", columnDefinition = "uuid", nullable = false)
    private UUID caseId;

    @Column(name = "document_id", columnDefinition = "uuid")
    private UUID documentId;

    @Column(name = "evidence_number", nullable = false, unique = true, length = 50)
    private String evidenceNumber;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", length = 5000)
    private String description;

    @Column(name = "evidence_type", length = 100)
    private String evidenceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private EvidenceStatus status = EvidenceStatus.COLLECTED;

    @Column(name = "collection_location", length = 1000)
    private String collectionLocation;

    @Column(name = "collected_at")
    private Instant collectedAt;

    @Column(name = "collected_by", columnDefinition = "uuid")
    private UUID collectedBy;

    @Column(name = "current_custodian_id", columnDefinition = "uuid")
    private UUID currentCustodianId;

    @Column(name = "chain_hash", length = 64)
    private String chainHash;

    @Column(name = "qr_code_data", length = 2000)
    private String qrCodeData;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "classification", length = 100)
    private String classification;

    @CreatedBy
    @Column(name = "created_by", columnDefinition = "uuid", updatable = false)
    private UUID createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
