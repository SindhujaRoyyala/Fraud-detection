package com.dems.timeline.repository;

import com.dems.timeline.model.TimelineEvent;
import com.dems.timeline.model.TimelineEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimelineEventRepository extends JpaRepository<TimelineEvent, UUID> {

    Page<TimelineEvent> findByCaseIdOrderByCreatedAtDesc(UUID caseId, Pageable pageable);

    Page<TimelineEvent> findByCaseIdAndEventTypeOrderByCreatedAtDesc(
            UUID caseId, TimelineEventType eventType, Pageable pageable);

    @Query("SELECT t FROM TimelineEvent t WHERE t.caseId = :caseId ORDER BY t.eventDate DESC, t.createdAt DESC")
    List<TimelineEvent> findByCaseIdChronological(@Param("caseId") UUID caseId);
}
