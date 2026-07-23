--add problem statement column
ALTER TABLE hackathon
ADD COLUMN IF NOT EXISTS problem_statement_storage_key TEXT;



ALTER TABLE submissions ADD COLUMN IF NOT EXISTS event_id UUID;

UPDATE submissions s
SET event_id = t.event_id
FROM teams t
WHERE s.team_id = t.team_id
  AND s.event_id IS NULL;

ALTER TABLE submissions ALTER COLUMN event_id SET NOT NULL;

ALTER TABLE submissions
    ADD CONSTRAINT fk_submissions_event_id
        FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE RESTRICT;

--Replace scoringlogs.hackathon_id with scoringlogs.event_id.
ALTER TABLE scoringlogs DROP CONSTRAINT IF EXISTS uq_scoringlogs_team_hackathon_level;
ALTER TABLE scoringlogs DROP CONSTRAINT IF EXISTS fk_scoringlogs_hackathon_id;

ALTER TABLE scoringlogs ADD COLUMN IF NOT EXISTS event_id UUID;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'scoringlogs' AND column_name = 'hackathon_id'
    ) THEN
        UPDATE scoringlogs sl
        SET event_id = t.event_id
        FROM teams t
        WHERE sl.team_id = t.team_id
          AND sl.event_id IS NULL;

        ALTER TABLE scoringlogs ALTER COLUMN hackathon_id DROP NOT NULL;
        ALTER TABLE scoringlogs DROP COLUMN hackathon_id;
    END IF;
END $$;

ALTER TABLE scoringlogs ALTER COLUMN event_id SET NOT NULL;

ALTER TABLE scoringlogs
    ADD CONSTRAINT uq_scoringlogs_team_event_level UNIQUE (team_id, event_id, level_id);

ALTER TABLE scoringlogs
    ADD CONSTRAINT fk_scoringlogs_event_id
        FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE;