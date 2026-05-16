CREATE TABLE users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    uuid            CHAR(36)        NOT NULL,
    username        VARCHAR(64)     NOT NULL,
    email           VARCHAR(128)    NOT NULL,
    password_hash   VARCHAR(100)    NOT NULL,
    first_name      VARCHAR(64),
    last_name       VARCHAR(64),
    status          VARCHAR(32)     NOT NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP(6)    NOT NULL,
    updated_at      TIMESTAMP(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_uuid     UNIQUE (uuid),
    CONSTRAINT uk_users_email    UNIQUE (email),
    CONSTRAINT uk_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX ix_users_email    ON users(email);
CREATE INDEX ix_users_username ON users(username);
CREATE INDEX ix_users_status   ON users(status);
