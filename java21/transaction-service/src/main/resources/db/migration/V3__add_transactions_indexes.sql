-- Read-path indexes. Kept in V3 (post-data-load) so the seeder isn't slowed by index maintenance.
CREATE INDEX ix_tx_from_account  ON transactions(from_account_uuid);
CREATE INDEX ix_tx_to_account    ON transactions(to_account_uuid);
CREATE INDEX ix_tx_status        ON transactions(status);
CREATE INDEX ix_tx_type          ON transactions(type);
CREATE INDEX ix_tx_created_at    ON transactions(created_at);

-- Composite helper for the "find by account, recent first" page query used by the controller.
CREATE INDEX ix_tx_from_created  ON transactions(from_account_uuid, created_at);
CREATE INDEX ix_tx_to_created    ON transactions(to_account_uuid, created_at);
