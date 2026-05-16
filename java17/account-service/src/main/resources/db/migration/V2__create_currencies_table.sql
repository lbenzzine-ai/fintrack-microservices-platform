CREATE TABLE currencies (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        CHAR(3)      NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    minor_units INT          NOT NULL DEFAULT 2,
    PRIMARY KEY (id),
    CONSTRAINT uk_currencies_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO currencies (code, name, minor_units) VALUES
    ('USD', 'US Dollar',           2),
    ('EUR', 'Euro',                2),
    ('GBP', 'British Pound',       2),
    ('JPY', 'Japanese Yen',        0),
    ('BTC', 'Bitcoin',             8),
    ('ETH', 'Ethereum',            8);
