package com.fintrack.account.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Minimal projection of user-service's UserResponse, used by the Feign client. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UserSnapshot(
        String uuid,
        String username,
        String email,
        String status
) {}
