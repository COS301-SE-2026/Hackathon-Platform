CREATE TABLE certificate_template (
    template_id UUID PRIMARY KEY,
    event_id UUID REFERENCES events(event_id) ON DELETE CASCADE,
    hackathon_id UUID NULL REFERENCES hackathon(hackathon_id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    background_storage_key TEXT NULL,
    layout JSONB NOT NULL,
    created_by_user_id UUID NOT NULL REFERENCES users(user_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_certificate_template_event ON certificate_template(event_id);

CREATE TABLE certificate_generation_run (
    run_id UUID PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
    template_id UUID NOT NULL REFERENCES certificate_template(template_id),
    scope VARCHAR(20) NOT NULL,
    top_n INT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_count INT NOT NULL DEFAULT 0,
    completed_count INT NOT NULL DEFAULT 0,
    error_message TEXT NULL,
    requested_by_user_id UUID NOT NULL REFERENCES users(user_id),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    completed_at TIMESTAMP WITH TIME ZONE NULL
);

CREATE TABLE certificate_issued (
    certificate_id UUID PRIMARY KEY,
    run_id UUID NOT NULL REFERENCES certificate_generation_run(run_id) ON DELETE CASCADE,
    template_id UUID NOT NULL REFERENCES certificate_template(template_id),
    event_id UUID NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
    team_id UUID NULL REFERENCES teams(team_id),
    user_id UUID REFERENCES users(user_id),
    certificate_type VARCHAR(20) NOT NULL,
    recipient_name VARCHAR(255) NOT NULL,
    rank_at_issue INT NULL,
    storage_key TEXT NOT NULL,
    verification_code VARCHAR(20) NOT NULL UNIQUE,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_certificate_issued_user ON certificate_issued(user_id);
CREATE INDEX idx_certificate_issued_team ON certificate_issued(team_id);
CREATE INDEX idx_certificate_issued_event ON certificate_issued(event_id);
CREATE INDEX idx_certificate_issued_verification ON certificate_issued(verification_code);