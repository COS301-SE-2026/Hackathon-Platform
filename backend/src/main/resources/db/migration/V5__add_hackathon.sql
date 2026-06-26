CREATE TABLE hackathon (
                        hackathon_id UUID PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        description TEXT
);

ALTER TABLE events
ADD COLUMN hackathon_id UUID NOT NULL;

ALTER TABLE events
ADD CONSTRAINT fk_event_hackathon
FOREIGN KEY (hackathon_id)
REFERENCES hackathon(hackathon_id)

ALTER TABLE events
DROP CONSTRAINT chk_events_duration;

ALTER TABLE events
ADD CONSTRAINT chk_events_duration
CHECK (status IN ('ACTIVE', 'INACTIVE', 'COMPLETED', 'CANCELED', 'DRAFT'));

ALTER TABLE levelfiles
DROP COLUMN file_type;