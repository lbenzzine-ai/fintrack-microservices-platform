package com.fintrack.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 64)
    private String firstName;

    @Size(max = 64)
    private String lastName;
}
