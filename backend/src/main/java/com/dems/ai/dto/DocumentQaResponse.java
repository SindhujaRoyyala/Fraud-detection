package com.dems.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentQaResponse {

    private String question;
    private String answer;
    private String model;
    private Instant processedAt;
    private Long processingTimeMs;
    private List<SourceReference> sources;
}
