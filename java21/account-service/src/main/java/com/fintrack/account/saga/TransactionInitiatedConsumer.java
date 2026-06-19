package com.fintrack.account.saga;

import com.fintrack.account.event.TransactionInitiatedEvent;
import com.fintrack.account.event.TransactionFailedEvent;
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
        log.info("[SAGA] transaction-initiated tx={} from={} to={} amount={}",
                event.transactionUuid(), event.fromAccountUuid(), event.toAccountUuid(), event.amount());
        try {
            // Step 1 — debit source account
            accountService.debit(event.fromAccountUuid(), event.transactionUuid(),
                    event.amount(), event.fee());

            // Step 2 — credit destination account
            if (event.toAccountUuid() != null && !event.toAccountUuid().isBlank()) {
                accountService.credit(event.toAccountUuid(), event.transactionUuid(),
                        event.amount());
            }

        } catch (InsufficientFundsException ex) {
            publishFailure(event, "INSUFFICIENT_FUNDS", ex.getMessage(), false);
        } catch (AccountFrozenException ex) {
            publishFailure(event, "ACCOUNT_FROZEN", ex.getMessage(), false);
        } catch (AccountNotFoundException ex) {
            publishFailure(event, "ACCOUNT_NOT_FOUND", ex.getMessage(), false);
        } catch (Exception ex) {
            log.error("Unexpected error processing tx-initiated for tx={}", event.transactionUuid(), ex);
            publishFailure(event, "DOWNSTREAM", ex.getMessage(), false);
        }
    }

    private void publishFailure(TransactionInitiatedEvent in, String code, String reason, boolean alreadyDebited) {
        TransactionFailedEvent out = TransactionFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionUuid(in.transactionUuid())
                .fromAccountUuid(in.fromAccountUuid())
                .toAccountUuid(in.toAccountUuid())
                .amount(in.amount())
                .fee(in.fee())
                .currencyCode(in.currencyCode())
                .reasonCode(code)
                .reason(reason)
                .alreadyDebited(alreadyDebited)
                .occurredAt(Instant.now())
                .build();
        messaging.active().publish(txFailedTopic, in.transactionUuid(), out);
        log.warn("[SAGA] published transaction-failed tx={} reason={}", in.transactionUuid(), code);
    }
}
