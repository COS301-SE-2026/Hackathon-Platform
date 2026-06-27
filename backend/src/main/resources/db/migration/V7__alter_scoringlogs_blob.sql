ALTER TABLE scoringlogs DROP CONSTRAINT IF EXISTS fk_scoringlogs_submission_id;

ALTER TABLE scoringlogs
    DROP COLUMN IF EXISTS submission_id,
    DROP COLUMN IF EXISTS log_text,
    DROP COLUMN IF EXISTS error_type;

ALTER TABLE scoringlogs
    ADD COLUMN team_id  UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN event_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN level_id UUID NOT NULL DEFAULT 0,
    ADD COLUMN storage_key VARCHAR(500) NOT NULL DEFAULT '',
    ADD COLUMN submission_count INT NOT NULL DEFAULT 0,
    ADD COLUMN last_updated_at TIMESTAMPZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

