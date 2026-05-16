package com.fintrack.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Java 21 — immutable {@code record} replacing the Lombok {@code @Data @Builder} DTO from the
 * Java 17 stack. The {@code builder()} method is preserved so callers stay source-compatible
 * across both stacks.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FallbackResponse(
        String service,
        String message,
        String hint,
        int status,
        Instant timestamp,
        String correlationId
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String service;
        private String message;
        private String hint;
        private int status;
        private Instant timestamp;
        private String correlationId;

        public Builder service(String v)        { this.service = v;        return this; }
        public Builder message(String v)        { this.message = v;        return this; }
        public Builder hint(String v)           { this.hint = v;           return this; }
        public Builder status(int v)            { this.status = v;         return this; }
        public Builder timestamp(Instant v)     { this.timestamp = v;      return this; }
        public Builder correlationId(String v)  { this.correlationId = v;  return this; }

        public FallbackResponse build() {
            return new FallbackResponse(service, message, hint, status, timestamp, correlationId);
        }
    }
}
