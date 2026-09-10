package com.dems.document.dto;

import com.dems.document.model.DocumentStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocumentRequest {

    @Size(max = 500)
    private String title;

    @Size(max = 5000)
    private String description;

    private DocumentStatus status;

    private UUID caseId;

    @Size(max = 2000)
    private String tags;

    @Size(max = 100)
    private String classification;

    private Boolean sensitive;
}
