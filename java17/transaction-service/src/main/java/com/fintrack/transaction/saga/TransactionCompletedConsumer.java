package com.fintrack.transaction.saga;

import com.fintrack.transaction.event.TransactionCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Self-consume of {@code transaction-completed} for audit/observability of saga terminations.
 * Other services (notification-service, analytics sinks) also consume this; we listen here to
 * keep a single source-of-truth log for the saga lifecycle in transaction-service.
 */
@Slf4j
@Component
public class TransactionCompletedConsumer {

    @KafkaListener(
        topics = "${fintrack.messaging.kafka.topics.transaction-completed}",
        groupId = "${spring.application.name}-orchestrator-completed",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransactionCompleted(@Payload TransactionCompletedEvent event) {
        log.info("[SAGA] terminal-trace tx-completed tx={} amount={} fee={} currency={}",
                event.getTransactionUuid(), event.getAmount(), event.getFee(), event.getCurrencyCode());
    }
}
