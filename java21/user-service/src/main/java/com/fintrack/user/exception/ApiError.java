package com.fintrack.user.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldViolation> violations
) {
    public static Builder builder() { return new Builder(); }

    public record FieldViolation(String field, String message) {
        public static FieldViolation of(String field, String message) {
            return new FieldViolation(field, message);
        }
    }

    public static final class Builder {
        private Instant timestamp;
        private int status;
        private String code;
        private String message;
        private String path;
        private List<FieldViolation> violations;

        public Builder timestamp(Instant v)             { this.timestamp = v;  return this; }
        public Builder status(int v)                    { this.status = v;     return this; }
        public Builder code(String v)                   { this.code = v;       return this; }
        public Builder message(String v)                { this.message = v;    return this; }
        public Builder path(String v)                   { this.path = v;       return this; }
        public Builder violations(List<FieldViolation> v) { this.violations = v; return this; }
        public ApiError build() { return new ApiError(timestamp, status, code, message, path, violations); }
    }
}
