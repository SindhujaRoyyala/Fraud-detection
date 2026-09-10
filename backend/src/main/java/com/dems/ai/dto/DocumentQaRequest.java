package com.dems.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentQaRequest {

    @NotBlank(message = "Question is required")
    private String question;

    private List<UUID> documentIds;

    private UUID caseId;

    @Builder.Default
    private Integer topK = 5;

    @Builder.Default
    private Boolean includeSources = true;
}
