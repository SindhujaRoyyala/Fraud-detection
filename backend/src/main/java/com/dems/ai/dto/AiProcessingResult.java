package com.dems.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiProcessingResult {

    private UUID documentId;
    private String status;
    private String ocrText;
    private String summary;
    private Object metadata;
    private Integer pageCount;
    private List<String> keyPhrases;
    private String entities;
    private Instant processedAt;
    private Long processingTimeMs;
    private Boolean embeddingsStored;
    private String errorMessage;
}
