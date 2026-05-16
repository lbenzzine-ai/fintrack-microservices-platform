package com.fintrack.transaction.saga;

import com.fintrack.transaction.event.TransactionFailedEvent;
import com.fintrack.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Saga compensation — when account-service emits {@code tx-failed} (debit refused) we mark the
 * transaction FAILED on our side. When the orchestrator itself emits {@code tx-failed} (downstream
 * error after debit), the listener flips the row to COMPENSATED once it observes the event.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionFailedConsumer {

    private final TransactionService transactionService;

    @KafkaListener(
        topics = "${fintrack.messaging.kafka.topics.transaction-failed}",
        groupId = "${spring.application.name}-orchestrator-failed",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransactionFailed(@Payload TransactionFailedEvent event) {
        log.warn("[SAGA] tx-failed tx={} reason={} alreadyDebited={}",
                event.getTransactionUuid(), event.getReasonCode(), event.isAlreadyDebited());
        if (event.isAlreadyDebited()) {
            transactionService.markCompensated(event.getTransactionUuid(),
                    event.getReasonCode(), event.getReason());
        } else {
            transactionService.markFailed(event.getTransactionUuid(),
                    event.getReasonCode(), event.getReason(), false);
        }
    }
}
