package com.dems.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultItem {

    private UUID documentId;
    private UUID caseId;
    private String caseNumber;
    private String title;
    private String fileName;
    private Float score;
    private String snippet;
    private Integer pageNumber;
    private String mimeType;
    private Long fileSizeBytes;
    private String classification;
}
