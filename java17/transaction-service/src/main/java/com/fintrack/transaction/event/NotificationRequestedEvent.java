package com.fintrack.transaction.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/** Fan-out signal asking notification-service to dispatch a notification to the user. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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
