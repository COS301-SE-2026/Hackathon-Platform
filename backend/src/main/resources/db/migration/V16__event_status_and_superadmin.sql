UPDATE events SET status = 'UPCOMING' WHERE status = 'UCOMING';
UPDATE events SET status = 'UPCOMING' WHERE status = 'INACTIVE';
ALTER TABLE events DROP CONSTRAINT IF EXISTS chk_events_status;
ALTER TABLE events ADD CONSTRAINT chk_events_status CHECK(status IN ('UPCOMING', 'ACTIVE', 'COMPLETED', 'CANCELED'));

ALTER TABLE events
    ADD COLUMN IF NOT EXISTS is_in_person BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS allowed_technologies JSONB NOT NULL DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS rules TEXT,
    ADD COLUMN IF NOT EXISTS tagline VARCHAR(255),
    ADD COLUMN IF NOT EXISTS first_place_prize NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS second_place_prize NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS third_place_prize NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_prize_pool NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS leaderboard_freeze_datetime TIMESTAMPTZ;

ALTER TABLE events DROP CONSTRAINT IF EXISTS chk_events_prize_nonnegative;
ALTER TABLE events
    ADD CONSTRAINT chk_events_prizes_nonnegative
    CHECK (first_place_prize >= 0 AND second_place_prize >=0 AND third_place_prize >= 0 AND total_prize_pool >= 0);

ALTER TABLE event_participants
    ADD COLUMN IF NOT EXISTS dietary_requirements TEXT,
    ADD COLUMN IF NOT EXISTS allergies TEXT;

INSERT INTO roles (name, description)
VALUES ('SUPERADMIN', 'Platform Super Administrator')
ON CONFLICT (name) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_events_status_start_datetime ON events(status, start_datetime);
CREATE INDEX IF NOT EXISTS idx_events_leaderboard_freeze_datetime ON events(leaderboard_freeze_datetime);