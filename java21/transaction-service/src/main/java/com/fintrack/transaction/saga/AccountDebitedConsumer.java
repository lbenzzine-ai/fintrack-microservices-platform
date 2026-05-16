package com.fintrack.transaction.saga;

import com.fintrack.transaction.event.AccountDebitedEvent;
import com.fintrack.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

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
                event.transactionUuid(), event.accountUuid(), event.newBalance());
        try {
            transactionService.markDebited(event.transactionUuid());
        } catch (Exception ex) {
            log.error("[SAGA] failed to advance tx={} after debit — compensating", event.transactionUuid(), ex);
            transactionService.markFailed(event.transactionUuid(),
                    "ORCHESTRATOR_ERROR", ex.getMessage(), true);
        }
    }
}
