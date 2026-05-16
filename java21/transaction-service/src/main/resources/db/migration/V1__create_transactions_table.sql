CREATE TABLE transactions (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    uuid                CHAR(36)       NOT NULL,
    from_account_uuid   CHAR(36)       NOT NULL,
    to_account_uuid     CHAR(36)       NULL,
    amount              DECIMAL(19,4)  NOT NULL,
    fee                 DECIMAL(19,4)  NOT NULL DEFAULT 0,
    currency_code       CHAR(3)        NOT NULL,
    type                VARCHAR(32)    NOT NULL,
    status              VARCHAR(16)    NOT NULL,
    description         VARCHAR(255)   NULL,
    failure_reason      VARCHAR(255)   NULL,
    version             BIGINT         NOT NULL DEFAULT 0,
    created_at          TIMESTAMP(6)   NOT NULL,
    updated_at          TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tx_uuid UNIQUE (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
