package com.dems.ai.dto;

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
public class SourceReference {

    private UUID documentId;
    private String documentTitle;
    private Integer pageNumber;
    private String fileName;
    private Float similarityScore;
    private String snippet;
}
