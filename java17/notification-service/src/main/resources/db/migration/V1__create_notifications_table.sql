CREATE TABLE notifications (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    uuid              CHAR(36)       NOT NULL,
    account_uuid      CHAR(36)       NULL,
    transaction_uuid  CHAR(36)       NULL,
    recipient         VARCHAR(255)   NOT NULL,
    channel           VARCHAR(16)    NOT NULL,
    template          VARCHAR(64)    NULL,
    subject           VARCHAR(255)   NULL,
    body              TEXT           NULL,
    status            VARCHAR(16)    NOT NULL,
    failure_reason    VARCHAR(512)   NULL,
    delivery_provider VARCHAR(64)    NULL,
    sent_at           TIMESTAMP(6)   NULL,
    version           BIGINT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMP(6)   NOT NULL,
    updated_at        TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notif_uuid UNIQUE (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_notif_account ON notifications(account_uuid);
CREATE INDEX ix_notif_status  ON notifications(status);
CREATE INDEX ix_notif_channel ON notifications(channel);
CREATE INDEX ix_notif_tx      ON notifications(transaction_uuid);
CREATE INDEX ix_notif_created ON notifications(created_at);
