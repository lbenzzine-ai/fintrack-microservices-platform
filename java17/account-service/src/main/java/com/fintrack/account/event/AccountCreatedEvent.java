package com.fintrack.account.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountCreatedEvent implements Serializable {
    private String eventId;
    private String accountUuid;
    private String userUuid;
    private BigDecimal openingBalance;
    private String currencyCode;
    private Instant occurredAt;
}
