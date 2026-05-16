package com.fintrack.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LoginResponse {
    private String tokenType;
    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;
    private UserResponse user;
}
