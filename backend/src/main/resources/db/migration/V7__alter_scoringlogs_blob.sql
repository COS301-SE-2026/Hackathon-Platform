ALTER TABLE scoringlogs DROP CONSTRAINT IF EXISTS fk_scoringlogs_submission_id;

ALTER TABLE scoringlogs
    DROP COLUMN IF EXISTS submission_id,
    DROP COLUMN IF EXISTS log_text,
    DROP COLUMN IF EXISTS error_type;

ALTER TABLE scoringlogs
    ADD COLUMN team_id  UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN event_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN level_id BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN storage_key VARCHAR(500) NOT NULL DEFAULT '',
    ADD COLUMN submission_count INT NOT NULL DEFAULT 0,
    ADD COLUMN last_updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE scoringlogs
    ALTER COLUMN team_id DROP DEFAULT,
    ALTER COLUMN event_id DROP DEFAULT,
    ALTER COLUMN level_id DROP DEFAULT,
    ALTER COLUMN storage_key DROP DEFAULT;

ALTER TABLE scoringlogs
    ADD CONSTRAINT uq_scoringlogs_team_event_level UNIQUE (team_id, event_id, level_id);

ALTER TABLE scoringlogs
    ADD CONSTRAINT fk_scoringlogs_team_id
        FOREIGN KEY (team_id) REFERENCES  teams(team_id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_scoringlogs_event_id
        FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE;