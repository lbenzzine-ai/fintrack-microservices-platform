package com.fintrack.user.dto;

import com.fintrack.user.entity.UserStatus;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

public record UserResponse(
        String uuid,
        String username,
        String email,
        String firstName,
        String lastName,
        UserStatus status,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String uuid;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private UserStatus status;
        private Set<String> roles;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder uuid(String v)             { this.uuid = v;        return this; }
        public Builder username(String v)         { this.username = v;    return this; }
        public Builder email(String v)            { this.email = v;       return this; }
        public Builder firstName(String v)        { this.firstName = v;   return this; }
        public Builder lastName(String v)         { this.lastName = v;    return this; }
        public Builder status(UserStatus v)       { this.status = v;      return this; }
        public Builder roles(Set<String> v)       { this.roles = v;       return this; }
        public Builder createdAt(Instant v)       { this.createdAt = v;   return this; }
        public Builder updatedAt(Instant v)       { this.updatedAt = v;   return this; }
        public UserResponse build() {
            return new UserResponse(uuid, username, email, firstName, lastName, status, roles, createdAt, updatedAt);
        }
    }
}
