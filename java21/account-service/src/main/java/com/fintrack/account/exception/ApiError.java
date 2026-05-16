package com.fintrack.account.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Instant timestamp; private int status; private String code; private String message; private String path;
        public Builder timestamp(Instant v) { this.timestamp = v; return this; }
        public Builder status(int v)        { this.status = v;    return this; }
        public Builder code(String v)       { this.code = v;      return this; }
        public Builder message(String v)    { this.message = v;   return this; }
        public Builder path(String v)       { this.path = v;      return this; }
        public ApiError build() { return new ApiError(timestamp, status, code, message, path); }
    }
}
