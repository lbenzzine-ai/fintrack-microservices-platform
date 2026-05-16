package com.fintrack.account.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** Minimal projection of user-service's UserResponse, used by the Feign client. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSnapshot {
    private String uuid;
    private String username;
    private String email;
    private String status;
}
