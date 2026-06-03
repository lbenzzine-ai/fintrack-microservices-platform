DELETE FROM user_roles WHERE user_id = (SELECT id FROM users WHERE email = 'admin@fintrack.io');
DELETE FROM users WHERE email = 'admin@fintrack.io';