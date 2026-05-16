package com.fintrack.notification.strategy.channel;

import com.fintrack.notification.entity.Notification;
import com.fintrack.notification.entity.NotificationChannel;

/**
 * Java 21 — {@code sealed interface} locks the channel set at compile time so the registry can
 * verify exhaustiveness without reflection.
 */
public sealed interface NotificationStrategy
        permits EmailNotificationStrategy, SMSNotificationStrategy, PushNotificationStrategy {

    NotificationChannel channel();

    DispatchResult send(Notification notification);
}
