package com.fintrack.transaction.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
public class FeeQuote implements Serializable {
    private String strategy;
    private BigDecimal principal;
    private BigDecimal fee;
    private BigDecimal total;
}
