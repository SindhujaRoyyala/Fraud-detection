package com.dems.timeline.model;

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
@Table(name = "timeline_events", schema = "public", indexes = {
        @Index(columnList = "case_id"),
        @Index(columnList = "event_type"),
        @Index(columnList = "created_by"),
        @Index(columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class TimelineEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "case_id", columnDefinition = "uuid", nullable = false)
    private UUID caseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 100)
    private TimelineEventType eventType;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", length = 5000)
    private String description;

    @Column(name = "document_id", columnDefinition = "uuid")
    private UUID documentId;

    @Column(name = "evidence_id", columnDefinition = "uuid")
    private UUID evidenceId;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "event_date")
    private Instant eventDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
