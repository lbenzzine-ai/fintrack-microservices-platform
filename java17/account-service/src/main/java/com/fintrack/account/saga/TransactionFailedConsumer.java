package com.fintrack.account.saga;

import com.fintrack.account.event.TransactionFailedEvent;
import com.fintrack.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Saga compensation — on {@code transaction-failed} where {@code alreadyDebited=true}, re-credit
 * the source account. transaction-service emits this when a downstream step failed AFTER the
 * debit (e.g. crediting the destination errored). When {@code alreadyDebited=false} we ignore
 * the event since we never deducted in the first place.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionFailedConsumer {

    private final AccountService accountService;

    @KafkaListener(
        topics = "${fintrack.messaging.kafka.topics.transaction-failed}",
        groupId = "${spring.application.name}-compensation",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransactionFailed(@Payload TransactionFailedEvent event) {
        if (!event.isAlreadyDebited()) {
            log.debug("[SAGA] tx-failed tx={} not yet debited — no compensation needed", event.getTransactionUuid());
            return;
        }
        log.warn("[SAGA] compensating tx={} account={} reason={}",
                event.getTransactionUuid(), event.getFromAccountUuid(), event.getReasonCode());
        accountService.compensateCredit(event.getFromAccountUuid(), event.getAmount(), event.getFee(), event.getReasonCode());
    }
}
