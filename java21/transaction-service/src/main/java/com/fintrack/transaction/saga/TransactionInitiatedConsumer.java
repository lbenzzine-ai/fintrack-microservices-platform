package com.fintrack.transaction.saga;

import com.fintrack.transaction.event.TransactionInitiatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionInitiatedConsumer {

    @KafkaListener(
        topics = "${fintrack.messaging.kafka.topics.transaction-initiated}",
        groupId = "${spring.application.name}-orchestrator-init",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransactionInitiated(@Payload TransactionInitiatedEvent event) {
        log.info("[SAGA] orchestrator-trace tx-initiated tx={} from={} amount={}",
                event.transactionUuid(), event.fromAccountUuid(), event.amount());
    }
}
