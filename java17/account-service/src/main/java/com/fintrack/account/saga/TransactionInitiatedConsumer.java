package com.fintrack.account.saga;

import com.fintrack.account.event.TransactionFailedEvent;
import com.fintrack.account.event.TransactionInitiatedEvent;
import com.fintrack.account.exception.AccountFrozenException;
import com.fintrack.account.exception.AccountNotFoundException;
import com.fintrack.account.exception.InsufficientFundsException;
import com.fintrack.account.messaging.MessagingStrategyRegistry;
import com.fintrack.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Saga step 4 — on {@code transaction-initiated}, attempt to debit the source account.
 * On success, {@link AccountService#debit} publishes {@code account-debited}.
 * On business failure (insufficient funds / frozen / unknown), we publish {@code transaction-failed}
 * with {@code alreadyDebited=false} so transaction-service marks the tx as FAILED without compensation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionInitiatedConsumer {

    private final AccountService accountService;
    private final MessagingStrategyRegistry messaging;

    @Value("${fintrack.messaging.kafka.topics.transaction-failed:fintrack.tx.failed}")
    private String txFailedTopic;

    @KafkaListener(
        topics = "${fintrack.messaging.kafka.topics.transaction-initiated}",
        groupId = "${spring.application.name}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransactionInitiated(@Payload TransactionInitiatedEvent event) {
        log.info("[SAGA] transaction-initiated tx={} from={} amount={}",
                event.getTransactionUuid(), event.getFromAccountUuid(), event.getAmount());
        try {
            accountService.debit(event.getFromAccountUuid(), event.getTransactionUuid(),
                    event.getAmount(), event.getFee());
        } catch (InsufficientFundsException ex) {
            publishFailure(event, "INSUFFICIENT_FUNDS", ex.getMessage(), false);
        } catch (AccountFrozenException ex) {
            publishFailure(event, "ACCOUNT_FROZEN", ex.getMessage(), false);
        } catch (AccountNotFoundException ex) {
            publishFailure(event, "ACCOUNT_NOT_FOUND", ex.getMessage(), false);
        } catch (Exception ex) {
            log.error("Unexpected error processing tx-initiated for tx={}", event.getTransactionUuid(), ex);
            publishFailure(event, "DOWNSTREAM", ex.getMessage(), false);
        }
    }

    private void publishFailure(TransactionInitiatedEvent in, String code, String reason, boolean alreadyDebited) {
        TransactionFailedEvent out = TransactionFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionUuid(in.getTransactionUuid())
                .fromAccountUuid(in.getFromAccountUuid())
                .toAccountUuid(in.getToAccountUuid())
                .amount(in.getAmount())
                .fee(in.getFee())
                .currencyCode(in.getCurrencyCode())
                .reasonCode(code)
                .reason(reason)
                .alreadyDebited(alreadyDebited)
                .occurredAt(Instant.now())
                .build();
        messaging.active().publish(txFailedTopic, in.getTransactionUuid(), out);
        log.warn("[SAGA] published transaction-failed tx={} reason={}", in.getTransactionUuid(), code);
    }
}
