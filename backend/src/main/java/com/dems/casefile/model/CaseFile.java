package com.dems.casefile.model;

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
@Table(name = "case_files", schema = "public", indexes = {
        @Index(columnList = "status"),
        @Index(columnList = "priority"),
        @Index(columnList = "created_by"),
        @Index(columnList = "created_at"),
        @Index(columnList = "case_number", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class CaseFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "case_number", nullable = false, unique = true, length = 50)
    private String caseNumber;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", length = 5000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private CaseStatus status = CaseStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    @Builder.Default
    private CasePriority priority = CasePriority.MEDIUM;

    @Column(name = "case_type", length = 100)
    private String caseType;

    @Column(name = "jurisdiction", length = 200)
    private String jurisdiction;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "assigned_investigator_id", columnDefinition = "uuid")
    private UUID assignedInvestigatorId;

    @Column(name = "assigned_legal_officer_id", columnDefinition = "uuid")
    private UUID assignedLegalOfficerId;

    @Column(name = "opened_at")
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

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
