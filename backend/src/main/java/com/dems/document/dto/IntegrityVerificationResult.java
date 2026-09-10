package com.dems.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrityVerificationResult {

    private boolean valid;
    private String storedHash;
    private String computedHash;
    private Instant verifiedAt;
    private String message;
}
