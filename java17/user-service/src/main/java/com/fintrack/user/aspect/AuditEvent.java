package com.fintrack.user.aspect;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Structured audit row — written as JSON via the {@code AUDIT} logger so Logstash/Kibana parse
 * it as a single doc per event. Field names are deliberately stable across services.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEvent {
    private Instant timestamp;
    private String username;
    private String method;
    private String result;
    private String requestId;
}
