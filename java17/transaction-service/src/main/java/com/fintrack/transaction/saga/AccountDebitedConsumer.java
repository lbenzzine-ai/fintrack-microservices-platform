package com.fintrack.transaction.saga;

import com.fintrack.transaction.event.AccountDebitedEvent;
import com.fintrack.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Saga step — on {@code account-debited}, advance the transaction to DEBITED and trigger the
 * COMPLETED transition. Any failure to advance is caught here so we can publish a tx-failed
 * with {@code alreadyDebited=true}, allowing account-service to compensate.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountDebitedConsumer {

    private final TransactionService transactionService;

    @KafkaListener(
        topics = "${fintrack.messaging.kafka.topics.account-debited}",
        groupId = "${spring.application.name}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAccountDebited(@Payload AccountDebitedEvent event) {
        log.info("[SAGA] account-debited tx={} account={} newBalance={}",
                event.getTransactionUuid(), event.getAccountUuid(), event.getNewBalance());
        try {
            transactionService.markDebited(event.getTransactionUuid());
        } catch (Exception ex) {
            log.error("[SAGA] failed to advance tx={} after debit — compensating", event.getTransactionUuid(), ex);
            transactionService.markFailed(event.getTransactionUuid(),
                    "ORCHESTRATOR_ERROR", ex.getMessage(), true);
        }
    }
}
