package com.fintrack.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterUserRequest {

    @NotBlank @Size(min = 3, max = 64)
    private String username;

    @NotBlank @Email @Size(max = 128)
    private String email;

    @NotBlank @Size(min = 8, max = 100)
    private String password;

    @Size(max = 64)
    private String firstName;

    @Size(max = 64)
    private String lastName;
}
