package com.fintrack.transaction.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record FeeQuote(
        String strategy,
        BigDecimal principal,
        BigDecimal fee,
        BigDecimal total
) implements Serializable {}
