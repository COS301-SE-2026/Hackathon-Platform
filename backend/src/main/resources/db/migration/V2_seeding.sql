INSERT INTO roles (name, description) VALUES
('ADMIN', 'Platform Administrator'),
('PARTICIPANT', 'Event Contestant');

INSERT INTO users (user_id, first_name, last_name, email, password_hash, role_id) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'System', 'Admin', 'admin@system.local', 'hash_123', 1),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'Jane', 'Coder', 'jane@test.com', 'hash_456', 2);

INSERT INTO events (event_id, created_by_user_id, name, team_size_limit, duration, status) VALUES
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'Main Competition', 2, 7200, 'ACTIVE');

INSERT INTO teams (team_id, team_name, created_by_user_id, event_id) VALUES
('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'Debug Thugs', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13');

INSERT INTO teammembers (team_member_id, team_id, user_id) VALUES
('e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', 'd0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 'b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12');

INSERT INTO levels (event_id, name, level_number) VALUES
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'Phase One', 1);

INSERT INTO solverversion (event_id, uploaded_by, storage_key) VALUES
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'v1/solver-engine');

INSERT INTO submissions (team_id, level_id, solver_version_id, score, status, source_code_storage_key, output_storage_key) VALUES
('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 1, 1, 85.00, 'SCORED', 'src/code.zip', 'out/logs.txt');

INSERT INTO leaderboardentries (team_id, level_id, event_id, best_score, best_submission_id) VALUES
('d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', 1, 'c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 85.00, 1);