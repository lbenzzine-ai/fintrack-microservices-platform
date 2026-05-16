CREATE TABLE accounts (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    uuid            CHAR(36)       NOT NULL,
    user_uuid       CHAR(36)       NOT NULL,
    balance         DECIMAL(19,4)  NOT NULL DEFAULT 0,
    currency_code   CHAR(3)        NOT NULL DEFAULT 'USD',
    status          VARCHAR(16)    NOT NULL,
    version         BIGINT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMP(6)   NOT NULL,
    updated_at      TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_accounts_uuid      UNIQUE (uuid),
    CONSTRAINT uk_accounts_user_uuid UNIQUE (user_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_accounts_user_uuid ON accounts(user_uuid);
CREATE INDEX ix_accounts_status    ON accounts(status);
