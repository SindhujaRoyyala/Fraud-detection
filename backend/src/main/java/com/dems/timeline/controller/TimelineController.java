package com.dems.timeline.controller;

import com.dems.common.response.ApiResponse;
import com.dems.timeline.dto.CreateTimelineEventRequest;
import com.dems.timeline.dto.TimelineEventResponse;
import com.dems.timeline.model.TimelineEventType;
import com.dems.timeline.service.TimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/timeline")
@RequiredArgsConstructor
@Tag(name = "Investigation Timeline", description = "Timeline event tracking for cases")
public class TimelineController {

    private final TimelineService timelineService;

    @PostMapping
    @Operation(summary = "Add timeline event", description = "Add manual event to case investigation timeline")
    public ResponseEntity<ApiResponse<TimelineEventResponse>> createEvent(
            @Valid @RequestBody CreateTimelineEventRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Timeline event added",
                timelineService.createEvent(request)));
    }

    @GetMapping("/cases/{caseId}")
    @Operation(summary = "Get case timeline", description = "Paginated timeline events for a case")
    public ResponseEntity<ApiResponse<Page<TimelineEventResponse>>> getTimeline(
            @PathVariable UUID caseId,
            @RequestParam(required = false) TimelineEventType type,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(timelineService.getTimeline(caseId, type, pageable)));
    }

    @GetMapping("/cases/{caseId}/chronological")
    @Operation(summary = "Chronological timeline", description = "Full case timeline ordered by event date")
    public ResponseEntity<ApiResponse<List<TimelineEventResponse>>> getChronological(@PathVariable UUID caseId) {
        return ResponseEntity.ok(ApiResponse.ok(timelineService.getChronologicalTimeline(caseId)));
    }
}
