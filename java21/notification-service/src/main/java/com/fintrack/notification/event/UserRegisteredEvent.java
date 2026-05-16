package com.fintrack.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserRegisteredEvent(
        String eventId,
        String userUuid,
        String username,
        String email,
        Instant occurredAt
) implements Serializable {}
