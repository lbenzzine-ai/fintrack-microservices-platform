package com.fintrack.transaction.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/** Mirrors account-service's outbound event. Consumed to advance the saga to DEBITED. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountDebitedEvent implements Serializable {
    private String eventId;
    private String transactionUuid;
    private String accountUuid;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal newBalance;
    private String currencyCode;
    private Instant occurredAt;
}
