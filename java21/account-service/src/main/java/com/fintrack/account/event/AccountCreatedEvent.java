package com.fintrack.account.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountCreatedEvent(
        String eventId,
        String accountUuid,
        String userUuid,
        BigDecimal openingBalance,
        String currencyCode,
        Instant occurredAt
) implements Serializable {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String eventId;
        private String accountUuid;
        private String userUuid;
        private BigDecimal openingBalance;
        private String currencyCode;
        private Instant occurredAt;
        public Builder eventId(String v)            { this.eventId = v;        return this; }
        public Builder accountUuid(String v)        { this.accountUuid = v;    return this; }
        public Builder userUuid(String v)           { this.userUuid = v;       return this; }
        public Builder openingBalance(BigDecimal v) { this.openingBalance = v; return this; }
        public Builder currencyCode(String v)       { this.currencyCode = v;   return this; }
        public Builder occurredAt(Instant v)        { this.occurredAt = v;     return this; }
        public AccountCreatedEvent build() {
            return new AccountCreatedEvent(eventId, accountUuid, userUuid, openingBalance, currencyCode, occurredAt);
        }
    }
}
