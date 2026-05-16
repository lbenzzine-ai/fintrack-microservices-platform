package com.fintrack.account.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Emitted by transaction-service on saga failure and consumed by account-service
 * to compensate (re-credit the source account). Also re-used as the message we publish
 * ourselves if the debit fails (insufficient funds / frozen account).
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionFailedEvent implements Serializable {
    private String eventId;
    private String transactionUuid;
    private String fromAccountUuid;
    private String toAccountUuid;
    private BigDecimal amount;
    private BigDecimal fee;
    private String currencyCode;
    private String reasonCode;       // INSUFFICIENT_FUNDS | ACCOUNT_FROZEN | ACCOUNT_NOT_FOUND | DOWNSTREAM
    private String reason;
    private boolean alreadyDebited;  // if true, account-service must compensate
    private Instant occurredAt;
}
