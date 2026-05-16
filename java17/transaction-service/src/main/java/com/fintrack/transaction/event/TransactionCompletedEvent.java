package com.fintrack.transaction.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/** Terminal saga event — broadcast on full success. Consumed by notification-service. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionCompletedEvent implements Serializable {
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
