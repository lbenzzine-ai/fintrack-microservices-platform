package com.fintrack.account.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionFailedEvent(
        String eventId,
        String transactionUuid,
        String fromAccountUuid,
        String toAccountUuid,
        BigDecimal amount,
        BigDecimal fee,
        String currencyCode,
        String reasonCode,
        String reason,
        boolean alreadyDebited,
        Instant occurredAt
) implements Serializable {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String eventId, transactionUuid, fromAccountUuid, toAccountUuid, currencyCode, reasonCode, reason;
        private BigDecimal amount, fee;
        private boolean alreadyDebited;
        private Instant occurredAt;
        public Builder eventId(String v)             { this.eventId = v;          return this; }
        public Builder transactionUuid(String v)     { this.transactionUuid = v;  return this; }
        public Builder fromAccountUuid(String v)     { this.fromAccountUuid = v;  return this; }
        public Builder toAccountUuid(String v)       { this.toAccountUuid = v;    return this; }
        public Builder amount(BigDecimal v)          { this.amount = v;           return this; }
        public Builder fee(BigDecimal v)             { this.fee = v;              return this; }
        public Builder currencyCode(String v)        { this.currencyCode = v;     return this; }
        public Builder reasonCode(String v)          { this.reasonCode = v;       return this; }
        public Builder reason(String v)              { this.reason = v;           return this; }
        public Builder alreadyDebited(boolean v)     { this.alreadyDebited = v;   return this; }
        public Builder occurredAt(Instant v)         { this.occurredAt = v;       return this; }
        public TransactionFailedEvent build() {
            return new TransactionFailedEvent(eventId, transactionUuid, fromAccountUuid, toAccountUuid,
                    amount, fee, currencyCode, reasonCode, reason, alreadyDebited, occurredAt);
        }
    }
}
