package com.fintrack.account.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class BalanceResponse implements Serializable {
    private String accountUuid;
    private BigDecimal balance;
    private String currencyCode;
}
