package com.fintrack.transaction.dto;

import com.fintrack.transaction.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateTransactionRequest(
        @NotBlank @Size(min = 36, max = 36) String fromAccountUuid,
        @Size(min = 36, max = 36) String toAccountUuid,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currencyCode,
        @NotNull TransactionType type,
        @Size(max = 255) String description
) {}
