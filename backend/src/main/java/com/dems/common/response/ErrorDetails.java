package com.dems.common.response;

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
public class ErrorDetails {

    private String code;
    private String message;
    private String path;
    private Instant timestamp;
    private List<String> details;

    public static ErrorDetails of(String code, String message, String path) {
        return ErrorDetails.builder()
                .code(code)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .build();
    }

    public static ErrorDetails of(String code, String message, String path, List<String> details) {
        return ErrorDetails.builder()
                .code(code)
                .message(message)
                .path(path)
                .timestamp(Instant.now())
                .details(details)
                .build();
    }
}
