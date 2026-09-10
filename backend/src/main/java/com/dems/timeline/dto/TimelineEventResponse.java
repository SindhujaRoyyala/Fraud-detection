package com.dems.timeline.dto;

import com.dems.timeline.model.TimelineEventType;
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
public class TimelineEventResponse {

    private UUID id;
    private UUID caseId;
    private TimelineEventType eventType;
    private String title;
    private String description;
    private UUID documentId;
    private UUID evidenceId;
    private UUID createdBy;
    private String createdByName;
    private String metadata;
    private Instant eventDate;
    private Instant createdAt;
}
