CREATE TABLE notification_preferences (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_uuid       CHAR(36)     NOT NULL,
    email_enabled   BOOLEAN      NOT NULL DEFAULT TRUE,
    sms_enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
    push_enabled    BOOLEAN      NOT NULL DEFAULT FALSE,
    email_address   VARCHAR(255) NULL,
    phone_number    VARCHAR(32)  NULL,
    push_token      VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notif_pref_user UNIQUE (user_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_notif_pref_user ON notification_preferences(user_uuid);
