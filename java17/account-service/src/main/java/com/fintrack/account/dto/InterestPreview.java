package com.fintrack.account.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class InterestPreview {
    private String strategy;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal projectedBalance;
}
