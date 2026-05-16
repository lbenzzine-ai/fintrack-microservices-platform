package com.fintrack.transaction.dto;

import com.fintrack.transaction.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateTransactionRequest {

    @NotBlank @Size(min = 36, max = 36)
    private String fromAccountUuid;

    /** Optional for ATM_WITHDRAWAL / BILL_PAYMENT. */
    @Size(min = 36, max = 36)
    private String toAccountUuid;

    @NotNull @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank @Size(min = 3, max = 3)
    private String currencyCode;

    @NotNull
    private TransactionType type;

    @Size(max = 255)
    private String description;
}
