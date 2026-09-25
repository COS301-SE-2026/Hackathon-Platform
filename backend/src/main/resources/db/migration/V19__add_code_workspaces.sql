ALTER TABLE teams
    ADD CONSTRAINT uq_teams_id_events UNIQUE (team_id, event_id);

ALTER TABLE events
    ADD CONSTRAINT uq_events_id_hackathon UNIQUE (event_id, hackathon_id);

ALTER TABLE levels
    ADD CONSTRAINT uq_levels_id_hackathon UNIQUE (id, hackathon_id);

CREATE TABLE code_workspaces (
    workspace_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL,
    team_id UUID NOT NULL,
    hackathon_id UUID NOT NULL,
    level_id SMALLINT NOT NULL,
    language VARCHAR(20) NOT NULL DEFAULT 'JAVA',
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_code_workspaces_event_team_level
        UNIQUE (event_id, team_id, level_id),

    CONSTRAINT fk_workspaces_team_events
        FOREIGN KEY (team_id, event_id)
        REFERENCES teams (team_id, event_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_code_workspaces_event_hackathon
        FOREIGN KEY (event_id, hackathon_id)
        REFERENCES events (event_id, hackathon_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_code_workspaces_level_hackathon
        FOREIGN KEY (level_id, hackathon_id)
        REFERENCES levels (id, hackathon_id)
        ON DELETE RESTRICT,
    
    CONSTRAINT fk_code_workspaces_Creator
        FOREIGN KEY (created_by_user_id)
        REFERENCES users (user_id)
        ON DELETE RESTRICT,
    
    CONSTRAINT chk_code_workspaces_language
        CHECK (language = 'JAVA')
);

CREATE INDEX inx_code_workspaces_team_event
    ON code_workspaces (team_id, event_id);

CREATE INDEX inx_code_workspaces_level_hackathon
    ON code_workspaces (level_id, hackathon_id);

CREATE INDEX inx_code_workspaces_creator
    ON code_workspaces (created_by_user_id);