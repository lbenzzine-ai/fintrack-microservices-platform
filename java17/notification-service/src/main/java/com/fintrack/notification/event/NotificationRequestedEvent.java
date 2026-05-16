package com.fintrack.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/** Mirrors transaction-service's outbound event. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationRequestedEvent implements Serializable {
    private String eventId;
    private String transactionUuid;
    private String accountUuid;
    private String channel;
    private String template;
    private String subject;
    private Map<String, Object> payload;
    private Instant occurredAt;
}
