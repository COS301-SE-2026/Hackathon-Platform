INSERT INTO users (user_id, first_name, last_name, email, password_hash, role_id, status, created_at, email_verified, auth_provider)
VALUES ( gen_random_uuid(), 'E2E', 'Participant', 'e2e.participant@e2e-test.com', '$2a$12$d3/gy9uGWnDKACuLVr86LuarjrRqkjfyDmnUrjUa2y54PZKJaGuLC', (SELECT role_id FROM roles WHERE name = 'PARTICIPANT' LIMIT 1), 'ACTIVE', CURRENT_TIMESTAMP, TRUE, 'LOCAL')
ON CONFLICT (email) DO NOTHING;
