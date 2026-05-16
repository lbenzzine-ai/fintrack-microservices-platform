package com.fintrack.notification.aspect;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** Structured audit row — Java 21 record (immutable, trivially JSON-serialisable). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditEvent(
        Instant timestamp,
        String username,
        String method,
        String result,
        String requestId
) {}
