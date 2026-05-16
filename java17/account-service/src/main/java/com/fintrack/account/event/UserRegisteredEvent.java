package com.fintrack.account.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/** Consumed from {@code fintrack.user.registered}. Mirrors user-service's outbound DTO. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserRegisteredEvent implements Serializable {
    private String eventId;
    private String userUuid;
    private String username;
    private String email;
    private Instant occurredAt;
}
