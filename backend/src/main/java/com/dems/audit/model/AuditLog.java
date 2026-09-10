package com.dems.audit.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_logs", schema = "public", indexes = {
        @Index(columnList = "event"),
        @Index(columnList = "user_id"),
        @Index(columnList = "case_id"),
        @Index(columnList = "document_id"),
        @Index(columnList = "created_at"),
        @Index(columnList = "ip_address")
})
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event", nullable = false, length = 100)
    private AuditEvent event;

    @Column(name = "user_id", columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "case_id", columnDefinition = "uuid")
    private UUID caseId;

    @Column(name = "document_id", columnDefinition = "uuid")
    private UUID documentId;

    @Column(name = "evidence_id", columnDefinition = "uuid")
    private UUID evidenceId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @Column(name = "description", length = 2000)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    @Column(name = "entry_hash", nullable = false, length = 64)
    private String entryHash;

    @Column(name = "block_number", nullable = false)
    private Long blockNumber;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
