package com.fintrack.user.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.Instant;

/**
 * First event of the FinTrack saga (Java 21 — immutable record).
 * Published by user-service to Kafka topic {@code fintrack.user.registered}; account-service
 * consumes it to auto-create a default wallet for the new user.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserRegisteredEvent(
        String eventId,
        String userUuid,
        String username,
        String email,
        Instant occurredAt
) implements Serializable {}
