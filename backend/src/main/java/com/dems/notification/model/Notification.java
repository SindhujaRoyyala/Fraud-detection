package com.dems.notification.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications", schema = "public", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "type"),
        @Index(columnList = "is_read"),
        @Index(columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 100)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "message", length = 5000)
    private String message;

    @Column(name = "case_id", columnDefinition = "uuid")
    private UUID caseId;

    @Column(name = "document_id", columnDefinition = "uuid")
    private UUID documentId;

    @Column(name = "evidence_id", columnDefinition = "uuid")
    private UUID evidenceId;

    @Column(name = "related_url", length = 1000)
    private String relatedUrl;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean read = false;

    @Column(name = "priority", length = 50)
    @Builder.Default
    private String priority = "NORMAL";

    @Column(name = "read_at")
    private Instant readAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
