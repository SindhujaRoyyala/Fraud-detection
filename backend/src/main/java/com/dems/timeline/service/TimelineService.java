package com.dems.timeline.service;

import com.dems.casefile.model.CaseFile;
import com.dems.casefile.repository.CaseFileRepository;
import com.dems.common.exception.ResourceNotFoundException;
import com.dems.security.context.CurrentUser;
import com.dems.security.context.CurrentUserContext;
import com.dems.timeline.dto.CreateTimelineEventRequest;
import com.dems.timeline.dto.TimelineEventResponse;
import com.dems.timeline.model.TimelineEvent;
import com.dems.timeline.model.TimelineEventType;
import com.dems.timeline.repository.TimelineEventRepository;
import com.dems.user.model.User;
import com.dems.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineService {

    private final TimelineEventRepository timelineRepository;
    private final CaseFileRepository caseFileRepository;
    private final UserRepository userRepository;
    private final CurrentUserContext currentUserContext;

    @Transactional
    public TimelineEventResponse createEvent(CreateTimelineEventRequest request) {
        CurrentUser currentUser = currentUserContext.requireAuthenticatedUser();

        CaseFile cf = caseFileRepository.findById(request.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Case", request.getCaseId().toString()));

        TimelineEvent event = TimelineEvent.builder()
                .caseId(request.getCaseId())
                .eventType(request.getEventType())
                .title(request.getTitle())
                .description(request.getDescription())
                .documentId(request.getDocumentId())
                .evidenceId(request.getEvidenceId())
                .createdBy(currentUser.getUserId())
                .metadata(request.getMetadata())
                .eventDate(request.getEventDate() != null ? request.getEventDate() : Instant.now())
                .build();

        TimelineEvent saved = timelineRepository.save(event);
        log.debug("Timeline event created for case {}: {}", cf.getCaseNumber(), event.getEventType());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TimelineEventResponse> getTimeline(UUID caseId, TimelineEventType type, Pageable pageable) {
        currentUserContext.requireAuthenticatedUser();

        if (!caseFileRepository.existsById(caseId)) {
            throw new ResourceNotFoundException("Case", caseId.toString());
        }

        Page<TimelineEvent> events;
        if (type != null) {
            events = timelineRepository.findByCaseIdAndEventTypeOrderByCreatedAtDesc(caseId, type, pageable);
        } else {
            events = timelineRepository.findByCaseIdOrderByCreatedAtDesc(caseId, pageable);
        }
        return events.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<TimelineEventResponse> getChronologicalTimeline(UUID caseId) {
        currentUserContext.requireAuthenticatedUser();
        if (!caseFileRepository.existsById(caseId)) {
            throw new ResourceNotFoundException("Case", caseId.toString());
        }
        return timelineRepository.findByCaseIdChronological(caseId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public void logAutoEvent(UUID caseId, TimelineEventType type, String title, String description,
                              UUID createdBy, UUID documentId, UUID evidenceId) {
        try {
            TimelineEvent event = TimelineEvent.builder()
                    .caseId(caseId)
                    .eventType(type)
                    .title(title)
                    .description(description)
                    .documentId(documentId)
                    .evidenceId(evidenceId)
                    .createdBy(createdBy)
                    .eventDate(Instant.now())
                    .build();
            timelineRepository.save(event);
        } catch (Exception e) {
            log.warn("Could not create timeline event: {}", e.getMessage());
        }
    }

    private TimelineEventResponse toResponse(TimelineEvent e) {
        String creatorName = null;
        if (e.getCreatedBy() != null) {
            Optional<User> u = userRepository.findById(e.getCreatedBy());
            if (u.isPresent()) {
                creatorName = u.get().getFullName() != null ? u.get().getFullName() : u.get().getUsername();
            }
        }
        return TimelineEventResponse.builder()
                .id(e.getId())
                .caseId(e.getCaseId())
                .eventType(e.getEventType())
                .title(e.getTitle())
                .description(e.getDescription())
                .documentId(e.getDocumentId())
                .evidenceId(e.getEvidenceId())
                .createdBy(e.getCreatedBy())
                .createdByName(creatorName)
                .metadata(e.getMetadata())
                .eventDate(e.getEventDate())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
