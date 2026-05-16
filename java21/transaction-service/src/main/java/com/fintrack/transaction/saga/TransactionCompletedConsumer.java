package com.fintrack.transaction.saga;

import com.fintrack.transaction.event.TransactionCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

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
                event.transactionUuid(), event.amount(), event.fee(), event.currencyCode());
    }
}
