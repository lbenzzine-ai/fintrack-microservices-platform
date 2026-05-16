package com.fintrack.account.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountDebitedEvent(
        String eventId,
        String transactionUuid,
        String accountUuid,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal newBalance,
        String currencyCode,
        Instant occurredAt
) implements Serializable {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String eventId, transactionUuid, accountUuid, currencyCode;
        private BigDecimal amount, fee, newBalance;
        private Instant occurredAt;
        public Builder eventId(String v)            { this.eventId = v;         return this; }
        public Builder transactionUuid(String v)    { this.transactionUuid = v; return this; }
        public Builder accountUuid(String v)        { this.accountUuid = v;     return this; }
        public Builder amount(BigDecimal v)         { this.amount = v;          return this; }
        public Builder fee(BigDecimal v)            { this.fee = v;             return this; }
        public Builder newBalance(BigDecimal v)     { this.newBalance = v;      return this; }
        public Builder currencyCode(String v)       { this.currencyCode = v;    return this; }
        public Builder occurredAt(Instant v)        { this.occurredAt = v;      return this; }
        public AccountDebitedEvent build() {
            return new AccountDebitedEvent(eventId, transactionUuid, accountUuid, amount, fee, newBalance, currencyCode, occurredAt);
        }
    }
}
