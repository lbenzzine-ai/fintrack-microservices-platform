package com.fintrack.notification.strategy.channel;

import com.fintrack.notification.entity.Notification;
import com.fintrack.notification.entity.NotificationChannel;

/**
 * Strategy contract — one impl per {@link NotificationChannel}. Implementations are responsible
 * for delivery and for stamping {@link DispatchResult} so the orchestrator can audit it.
 *
 * <p>Java 17 — plain interface. Java 21 build is a {@code sealed interface} with explicit
 * {@code permits}, locking the channel set at compile time.
 */
public interface NotificationStrategy {

    NotificationChannel channel();

    DispatchResult send(Notification notification);
}
