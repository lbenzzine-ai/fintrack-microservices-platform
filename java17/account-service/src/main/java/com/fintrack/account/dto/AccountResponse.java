package com.fintrack.account.dto;

import com.fintrack.account.entity.AccountStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class AccountResponse implements Serializable {
    private String uuid;
    private String userUuid;
    private BigDecimal balance;
    private String currencyCode;
    private AccountStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
