package com.dems.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResult {

    private DocumentResponse document;
    private boolean duplicate;
    private String duplicateOfDocumentId;
}
