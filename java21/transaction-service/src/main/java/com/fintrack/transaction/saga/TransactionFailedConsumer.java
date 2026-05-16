package com.fintrack.transaction.saga;

import com.fintrack.transaction.event.TransactionFailedEvent;
import com.fintrack.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

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
                event.transactionUuid(), event.reasonCode(), event.alreadyDebited());
        if (event.alreadyDebited()) {
            transactionService.markCompensated(event.transactionUuid(),
                    event.reasonCode(), event.reason());
        } else {
            transactionService.markFailed(event.transactionUuid(),
                    event.reasonCode(), event.reason(), false);
        }
    }
}
