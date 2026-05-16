package com.fintrack.notification.service;

import com.fintrack.notification.dto.NotificationResponse;
import com.fintrack.notification.dto.SendNotificationRequest;
import com.fintrack.notification.entity.Notification;
import com.fintrack.notification.entity.NotificationChannel;
import com.fintrack.notification.entity.NotificationPreference;
import com.fintrack.notification.entity.NotificationStatus;
import com.fintrack.notification.exception.NotificationNotFoundException;
import com.fintrack.notification.mapper.NotificationMapper;
import com.fintrack.notification.repository.NotificationPreferenceRepository;
import com.fintrack.notification.repository.NotificationRepository;
import com.fintrack.notification.strategy.channel.DispatchResult;
import com.fintrack.notification.strategy.channel.NotificationStrategyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates notification persistence + delivery. Templates are rendered via Thymeleaf so
 * marketing can iterate without redeploying; the dispatcher picks a {@link NotificationStrategyContext}
 * Strategy for the requested channel.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationMapper mapper;
    private final NotificationStrategyContext strategyContext;
    private final TemplateEngine templateEngine;

    /** Persist + dispatch synchronously. Returns the row with terminal status set. */
    @Transactional
    public NotificationResponse create(SendNotificationRequest req) {
        Notification n = Notification.builder()
                .uuid(UUID.randomUUID().toString())
                .accountUuid(req.getAccountUuid())
                .transactionUuid(req.getTransactionUuid())
                .recipient(req.getRecipient())
                .channel(req.getChannel())
                .template(req.getTemplate())
                .subject(req.getSubject())
                .body(renderBody(req.getTemplate(), req.getPayload()))
                .status(NotificationStatus.PENDING)
                .build();
        n = repository.save(n);
        dispatch(n);
        return mapper.toResponse(n);
    }

    /** Async path — Kafka consumers use this so the listener thread isn't blocked on SMTP. */
    @Async
    @Transactional
    public void createAsync(SendNotificationRequest req) {
        create(req);
    }

    @Transactional(readOnly = true)
    public NotificationResponse findByUuid(String uuid) {
        return repository.findByUuid(uuid).map(mapper::toResponse)
                .orElseThrow(() -> new NotificationNotFoundException(uuid));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> findByAccount(String accountUuid, Pageable pageable) {
        return repository.findByAccountUuid(accountUuid, pageable).map(mapper::toResponse);
    }

    @Transactional
    public NotificationPreference upsertPreference(String userUuid, NotificationPreference incoming) {
        return preferenceRepository.findByUserUuid(userUuid)
                .map(existing -> mergeAndSave(existing, incoming))
                .orElseGet(() -> {
                    incoming.setUserUuid(userUuid);
                    return preferenceRepository.save(incoming);
                });
    }

    public Optional<NotificationPreference> findPreference(String userUuid) {
        return preferenceRepository.findByUserUuid(userUuid);
    }

    /** Test the user's opt-in flags before dispatch. */
    public boolean isOptedIn(String userUuid, NotificationChannel channel) {
        return preferenceRepository.findByUserUuid(userUuid)
                .map(p -> switch (channel) {
                    case EMAIL -> p.isEmailEnabled();
                    case SMS -> p.isSmsEnabled();
                    case PUSH -> p.isPushEnabled();
                })
                .orElse(true);
    }

    private void dispatch(Notification n) {
        DispatchResult result = strategyContext.strategyFor(n.getChannel()).send(n);
        n.setDeliveryProvider(result.getProvider());
        if (result.isDelivered()) {
            n.setStatus(NotificationStatus.SENT);
            n.setSentAt(Instant.now());
            log.info("Notification {} delivered via {}", n.getUuid(), n.getChannel());
        } else {
            n.setStatus(NotificationStatus.FAILED);
            n.setFailureReason(result.getFailureReason());
            log.warn("Notification {} failed via {}: {}", n.getUuid(), n.getChannel(), result.getFailureReason());
        }
        repository.save(n);
    }

    private String renderBody(String template, Map<String, Object> payload) {
        if (template == null || template.isBlank()) return null;
        Context ctx = new Context();
        if (payload != null) payload.forEach(ctx::setVariable);
        try {
            return templateEngine.process(template, ctx);
        } catch (Exception ex) {
            // If template missing, fall back to inline rendering so the user still gets *something*.
            log.warn("Template '{}' render failed, falling back to inline body: {}", template, ex.getMessage());
            return fallback(template, payload);
        }
    }

    private String fallback(String template, Map<String, Object> payload) {
        Map<String, Object> safe = payload == null ? new HashMap<>() : payload;
        return "[" + template + "] " + safe;
    }

    private NotificationPreference mergeAndSave(NotificationPreference existing, NotificationPreference patch) {
        if (patch.getEmailAddress() != null) existing.setEmailAddress(patch.getEmailAddress());
        if (patch.getPhoneNumber()  != null) existing.setPhoneNumber(patch.getPhoneNumber());
        if (patch.getPushToken()    != null) existing.setPushToken(patch.getPushToken());
        existing.setEmailEnabled(patch.isEmailEnabled());
        existing.setSmsEnabled(patch.isSmsEnabled());
        existing.setPushEnabled(patch.isPushEnabled());
        return preferenceRepository.save(existing);
    }
}
