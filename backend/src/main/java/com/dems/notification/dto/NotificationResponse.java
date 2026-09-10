package com.dems.notification.dto;

import com.dems.notification.model.NotificationType;
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
public class NotificationResponse {

    private UUID id;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String message;
    private UUID caseId;
    private UUID documentId;
    private UUID evidenceId;
    private String relatedUrl;
    private String metadata;
    private Boolean read;
    private String priority;
    private Instant readAt;
    private Instant createdAt;
}
