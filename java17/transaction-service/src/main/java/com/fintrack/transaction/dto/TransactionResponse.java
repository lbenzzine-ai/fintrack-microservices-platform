package com.fintrack.transaction.dto;

import com.fintrack.transaction.entity.TransactionStatus;
import com.fintrack.transaction.entity.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransactionResponse implements Serializable {
    private String uuid;
    private String fromAccountUuid;
    private String toAccountUuid;
    private BigDecimal amount;
    private BigDecimal fee;
    private String currencyCode;
    private TransactionType type;
    private TransactionStatus status;
    private String riskLevel;
    private String description;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
}
