package com.fintrack.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountCreatedEvent implements Serializable {
    private String eventId;
    private String accountUuid;
    private String userUuid;
    private BigDecimal openingBalance;
    private String currencyCode;
    private Instant occurredAt;
}
