package com.fintrack.notification.exception;

import org.springframework.http.HttpStatus;

public class NotificationNotFoundException extends ApiException {
    public NotificationNotFoundException(String identifier) {
        super(HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND", "Notification not found: " + identifier);
    }
}
