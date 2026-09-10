package com.dems.timeline.dto;

import com.dems.timeline.model.TimelineEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateTimelineEventRequest {

    @NotNull(message = "Case ID is required")
    private UUID caseId;

    @NotNull(message = "Event type is required")
    private TimelineEventType eventType;

    @NotBlank(message = "Title is required")
    @Size(max = 500)
    private String title;

    @Size(max = 5000)
    private String description;

    private UUID documentId;

    private UUID evidenceId;

    private Instant eventDate;

    private String metadata;
}
