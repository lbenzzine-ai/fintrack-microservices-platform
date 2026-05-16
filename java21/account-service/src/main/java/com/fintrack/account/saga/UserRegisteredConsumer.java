package com.fintrack.account.saga;

import com.fintrack.account.event.UserRegisteredEvent;
import com.fintrack.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Saga step 2 — on {@code user-registered}, auto-create a default wallet so the
 * user can be debited/credited as soon as they exist.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredConsumer {

    private final AccountService accountService;

    @Value("${fintrack.account.default-currency:USD}")
    private String defaultCurrency;

    @KafkaListener(
        topics = "${fintrack.messaging.kafka.topics.user-registered}",
        groupId = "${spring.application.name}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserRegistered(@Payload UserRegisteredEvent event) {
        log.info("[SAGA] user-registered eventId={} userUuid={}", event.eventId(), event.userUuid());
        accountService.createForUser(event.userUuid(), defaultCurrency);
    }
}
