package com.fintrack.user.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * First event of the FinTrack saga. Published by user-service to Kafka topic
 * {@code fintrack.user.registered} on successful registration. account-service consumes
 * it and auto-creates a default wallet for the new user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRegisteredEvent implements Serializable {
    private String eventId;
    private String userUuid;
    private String username;
    private String email;
    private Instant occurredAt;
}
