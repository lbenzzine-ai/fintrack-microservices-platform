package com.fintrack.transaction.event;

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
 * Saga failure event. Read in two directions:
 * <ul>
 *   <li>account-service publishes this when the debit fails (alreadyDebited=false).</li>
 *   <li>transaction-service publishes this when a downstream step fails after the debit
 *       (alreadyDebited=true → account-service must compensate).</li>
 * </ul>
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
    private String reasonCode;
    private String reason;
    private boolean alreadyDebited;
    private Instant occurredAt;
}
