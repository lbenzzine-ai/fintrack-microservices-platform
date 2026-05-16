package com.fintrack.user.dto;

import com.fintrack.user.entity.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

@Data
@Builder
public class UserResponse implements Serializable {
    private String uuid;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserStatus status;
    private Set<String> roles;
    private Instant createdAt;
    private Instant updatedAt;
}
