CREATE TABLE fee_audits (
    id                BIGINT         NOT NULL AUTO_INCREMENT,
    transaction_uuid  CHAR(36)       NOT NULL,
    strategy          VARCHAR(32)    NOT NULL,
    principal         DECIMAL(19,4)  NOT NULL,
    fee               DECIMAL(19,4)  NOT NULL,
    created_at        TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_fee_audits_tx_uuid ON fee_audits(transaction_uuid);
