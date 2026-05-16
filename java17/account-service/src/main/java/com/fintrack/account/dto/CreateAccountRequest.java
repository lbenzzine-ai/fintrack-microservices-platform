package com.fintrack.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAccountRequest {

    @NotBlank @Size(min = 3, max = 3)
    private String currencyCode;
}
