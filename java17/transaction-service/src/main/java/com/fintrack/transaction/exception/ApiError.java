package com.fintrack.transaction.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private Instant timestamp;
    private int status;
    private String code;
    private String message;
    private String path;
    private List<FieldViolation> violations;

    @Data
    @Builder
    public static class FieldViolation {
        private String field;
        private String message;
}
}
