INSERT INTO users (user_id, first_name, last_name, email, password_hash, role_id, status, created_at) 
VALUES (
    gen_random_uuid(),
    'Demo',
    'Admin', 
    'demo@admin.com',
    '$2a$12$yTSUOj/eM8TCZhyhGppE1e4oROUfKMSBLqG.vUv8DaxFMPNBH55rm',
    (SELECT role_id FROM roles WHERE name = 'ADMIN' LIMIT 1),
    'ACTIVE',
    CURRENT_TIMESTAMP
);