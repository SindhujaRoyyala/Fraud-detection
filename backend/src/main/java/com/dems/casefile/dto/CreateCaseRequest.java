package com.dems.casefile.dto;

import com.dems.casefile.model.CasePriority;
import com.dems.casefile.model.CaseStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCaseRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title cannot exceed 500 characters")
    private String title;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;

    @Builder.Default
    private CaseStatus status = CaseStatus.DRAFT;

    @Builder.Default
    private CasePriority priority = CasePriority.MEDIUM;

    @Size(max = 100)
    private String caseType;

    @Size(max = 200)
    private String jurisdiction;

    private String metadata;
}
