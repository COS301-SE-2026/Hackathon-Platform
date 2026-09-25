CREATE TABLE ide_telemetry_session (
    session_id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL,
    user_id UUID NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP,

    CONSTRAINT fk_telemetry_session_workspace 
        FOREIGN KEY (workspace_id) 
        REFERENCES code_workspaces(workspace_id),
    
    CONSTRAINT fk_telemetry_session_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
);

CREATE TABLE ide_telemetry_event (
    event_id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    workspace_id UUID NOT NULL,
    user_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    client_timestamp TIMESTAMP NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sequence_number BIGINT NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT fk_telemetry_event_session
        FOREIGN KEY (session_id)
        REFERENCES ide_telemetry_session(session_id),

    CONSTRAINT fk_telemetry_event_workspace
        FOREIGN KEY (workspace_id)
        REFERENCES code_workspaces(workspace_id),
    
    CONSTRAINT fk_telemetry_event_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id),

    CONSTRAINT uq_telemetry_session_sequence
        UNIQUE (session_id, sequence_number)
);

CREATE INDEX idx_telemetry_event_session
    ON ide_telemetry_event(session_id);

CREATE INDEX idx_telemetry_event_workspace_user
    ON ide_telemetry_event(workspace_id, user_id);

CREATE INDEX idx_telemetry_event_type
    ON ide_telemetry_event(event_type);

CREATE INDEX idx_telemetry_event_received
    ON ide_telemetry_event(received_at);