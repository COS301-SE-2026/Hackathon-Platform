

ALTER TABLE scoringlogs DROP CONSTRAINT IF EXISTS uq_scoringlogs_team_event_level;

ALTER TABLE scoringlogs ADD COLUMN IF NOT EXISTS submission_id BIGINT;

ALTER TABLE scoringlogs
    ADD CONSTRAINT fk_scoringlogs_submission_id
        FOREIGN KEY (submission_id) REFERENCES submissions(id) ON DELETE CASCADE;

ALTER TABLE scoringlogs
    ADD CONSTRAINT uq_scoringlogs_submission_id UNIQUE (submission_id);

ALTER TABLE scoringlogs DROP COLUMN IF EXISTS submission_count;
ALTER TABLE scoringlogs DROP COLUMN IF EXISTS last_updated_at;
