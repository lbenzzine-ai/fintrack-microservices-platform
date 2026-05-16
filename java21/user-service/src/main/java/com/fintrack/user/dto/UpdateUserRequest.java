package com.fintrack.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 64) String firstName,
        @Size(max = 64) String lastName
) {}
