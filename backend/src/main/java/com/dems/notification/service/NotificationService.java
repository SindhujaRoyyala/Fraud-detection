package com.dems.notification.service;

import com.dems.common.exception.ResourceNotFoundException;
import com.dems.notification.dto.NotificationResponse;
import com.dems.notification.model.Notification;
import com.dems.notification.model.NotificationType;
import com.dems.notification.repository.NotificationRepository;
import com.dems.security.context.CurrentUser;
import com.dems.security.context.CurrentUserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserContext currentUserContext;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Boolean read, Pageable pageable) {
        CurrentUser user = currentUserContext.requireAuthenticatedUser();
        Page<Notification> page = read != null
                ? notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(user.getUserId(), read, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getUserId(), pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUnreadCount() {
        CurrentUser user = currentUserContext.requireAuthenticatedUser();
        long count = notificationRepository.countByUserIdAndReadFalse(user.getUserId());
        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", count);
        result.put("userId", user.getUserId());
        return result;
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        CurrentUser user = currentUserContext.requireAuthenticatedUser();
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId.toString()));
        if (!n.getUserId().equals(user.getUserId())) {
            throw new ResourceNotFoundException("Notification", notificationId.toString());
        }
        n.setRead(true);
        n.setReadAt(Instant.now());
        notificationRepository.save(n);
    }

    @Transactional
    public int markAllAsRead() {
        CurrentUser user = currentUserContext.requireAuthenticatedUser();
        return notificationRepository.markAllAsRead(user.getUserId(), Instant.now());
    }

    @Async
    @Transactional
    public void sendNotification(UUID userId, NotificationType type, String title, String message,
                                  UUID caseId, UUID documentId, UUID evidenceId,
                                  String relatedUrl, String priority) {
        try {
            Notification n = Notification.builder()
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .message(message)
                    .caseId(caseId)
                    .documentId(documentId)
                    .evidenceId(evidenceId)
                    .relatedUrl(relatedUrl)
                    .priority(priority != null ? priority : "NORMAL")
                    .read(false)
                    .build();
            notificationRepository.save(n);
            log.debug("Notification sent to user {}: {}", userId, type);
        } catch (Exception e) {
            log.warn("Could not save notification for user {}: {}", userId, e.getMessage());
        }
    }

    public void sendSimple(UUID userId, NotificationType type, String title, String message, UUID caseId) {
        sendNotification(userId, type, title, message, caseId, null, null, null, null);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .caseId(n.getCaseId())
                .documentId(n.getDocumentId())
                .evidenceId(n.getEvidenceId())
                .relatedUrl(n.getRelatedUrl())
                .metadata(n.getMetadata())
                .read(n.getRead())
                .priority(n.getPriority())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
