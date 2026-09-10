package com.dems.search.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchRequest {

    @NotBlank(message = "Query is required")
    private String query;

    private UUID caseId;

    @Builder.Default
    private Integer topK = 10;

    @Builder.Default
    private Float minScore = 0.7f;

    private Boolean includeMetadata;
}
