-- Default role catalogue
INSERT INTO roles (name) VALUES ('USER'), ('ADMIN'), ('SUPPORT');

-- Bootstrap admin account.
-- password is `Admin@12345`, BCrypt strength 12.
INSERT INTO users (uuid, username, email, password_hash, first_name, last_name, status, version, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@fintrack.io',
    '$2a$12$pUEypeCwgUlQk2bA/sCEZuFmibhfRkLT2W4FaIA8YPCsg.kfvuWtm',
    'Platform',
    'Admin',
    'ACTIVE',
    0,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name IN ('USER','ADMIN');
