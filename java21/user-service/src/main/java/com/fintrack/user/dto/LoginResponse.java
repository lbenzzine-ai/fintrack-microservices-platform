package com.fintrack.user.dto;

import java.time.Instant;

public record LoginResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        Instant expiresAt,
        UserResponse user
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tokenType;
        private String accessToken;
        private String refreshToken;
        private Instant expiresAt;
        private UserResponse user;

        public Builder tokenType(String v)    { this.tokenType = v;    return this; }
        public Builder accessToken(String v)  { this.accessToken = v;  return this; }
        public Builder refreshToken(String v) { this.refreshToken = v; return this; }
        public Builder expiresAt(Instant v)   { this.expiresAt = v;    return this; }
        public Builder user(UserResponse v)   { this.user = v;         return this; }
        public LoginResponse build() {
            return new LoginResponse(tokenType, accessToken, refreshToken, expiresAt, user);
        }
    }
}
