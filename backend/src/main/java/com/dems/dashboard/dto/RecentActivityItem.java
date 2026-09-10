package com.dems.dashboard.dto;

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
public class RecentActivityItem {

    private UUID id;
    private String type;
    private String title;
    private String description;
    private UUID userId;
    private String userName;
    private UUID caseId;
    private String caseNumber;
    private UUID documentId;
    private Instant createdAt;
}
