UPDATE events SET status = 'ACTIVE' WHERE status = 'INACTIVE';

ALTER TABLE events DROP CONSTRAINT chk_events_status;

ALTER TABLE events
    ADD CONSTRAINT chk_events_status
    CHECK (status IN ('UCOMING', 'ACTIVE', 'COMPLETED', 'CANCELED'));

ALTER TABLE events ALTER COLUMN status SET DEFAULT 'UPCOMING';