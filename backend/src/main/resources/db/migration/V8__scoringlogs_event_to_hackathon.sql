-- Replace scoringlogs.event_id with scoringlogs.hackathon_id.

ALTER TABLE scoringlogs DROP CONSTRAINT IF EXISTS uq_scoringlogs_team_event_level;
ALTER TABLE scoringlogs DROP CONSTRAINT IF EXISTS fk_scoringlogs_event_id;

ALTER TABLE scoringlogs ADD COLUMN IF NOT EXISTS hackathon_id UUID;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'scoringlogs' AND column_name = 'event_id'
    ) THEN
        UPDATE scoringlogs sl
        SET hackathon_id = e.hackathon_id
        FROM events e
        WHERE sl.event_id = e.event_id
          AND sl.hackathon_id IS NULL;

      
        ALTER TABLE scoringlogs ALTER COLUMN event_id DROP NOT NULL;
        ALTER TABLE scoringlogs DROP COLUMN event_id;
    END IF;

END $$;

ALTER TABLE scoringlogs ALTER COLUMN hackathon_id SET NOT NULL;

ALTER TABLE scoringlogs
    ADD CONSTRAINT uq_scoringlogs_team_hackathon_level UNIQUE (team_id, hackathon_id, level_id);

ALTER TABLE scoringlogs
    ADD CONSTRAINT fk_scoringlogs_hackathon_id
        FOREIGN KEY (hackathon_id) REFERENCES hackathon(hackathon_id) ON DELETE CASCADE;