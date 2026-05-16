package com.fintrack.notification.strategy.channel;

import com.fintrack.notification.entity.NotificationChannel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Channel dispatcher — looks up the right {@link NotificationStrategy} for the requested
 * {@link NotificationChannel}. Strategies are injected as a list; each registers itself
 * via {@link NotificationStrategy#channel()}.
 */
@Slf4j
@Component
public class NotificationStrategyContext {

    private final Map<NotificationChannel, NotificationStrategy> byChannel = new EnumMap<>(NotificationChannel.class);

    public NotificationStrategyContext(List<NotificationStrategy> strategies) {
        for (NotificationStrategy s : strategies) {
            NotificationStrategy existing = byChannel.put(s.channel(), s);
            if (existing != null && existing != s) {
                throw new IllegalStateException("Duplicate NotificationStrategy for channel " + s.channel());
            }
        }
    }

    @PostConstruct
    void verify() {
        for (NotificationChannel ch : NotificationChannel.values()) {
            if (!byChannel.containsKey(ch)) {
                throw new IllegalStateException("No NotificationStrategy registered for channel " + ch);
            }
        }
        log.info("NotificationStrategyContext ready: {}", byChannel.keySet());
    }

    public NotificationStrategy strategyFor(NotificationChannel channel) {
        NotificationStrategy s = byChannel.get(channel);
        if (s == null) throw new IllegalStateException("No strategy for " + channel);
        return s;
    }
}
