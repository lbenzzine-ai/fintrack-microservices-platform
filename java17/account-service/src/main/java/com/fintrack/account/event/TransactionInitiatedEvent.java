package com.fintrack.account.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/** Consumed from {@code fintrack.tx.initiated}. Causes a debit on {@code fromAccountUuid}. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionInitiatedEvent implements Serializable {
    private String eventId;
    private String transactionUuid;
    private String fromAccountUuid;
    private String toAccountUuid;
    private BigDecimal amount;
    private BigDecimal fee;
    private String currencyCode;
    private String type;
    private Instant occurredAt;
}
