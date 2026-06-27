CREATE TABLE hackathon (
                        hackathon_id UUID PRIMARY KEY,
                        name VARCHAR(100) NOT NULL,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        description TEXT
);

INSERT INTO hackathon (hackathon_id, name, description)
VALUES ('f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', 'Default hackathon', 'auto created for migration');

ALTER TABLE events
ADD COLUMN hackathon_id UUID;
UPDATE events SET hackathon_id = 'f0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16';
ALTER TABLE events ALTER COLUMN hackathon_id SET NOT NULL;

ALTER TABLE events
ADD CONSTRAINT fk_event_hackathon
FOREIGN KEY (hackathon_id)
REFERENCES hackathon(hackathon_id);

ALTER TABLE events
DROP CONSTRAINT chk_events_status;

ALTER TABLE events
ADD CONSTRAINT chk_events_status
CHECK (status IN ('ACTIVE', 'INACTIVE', 'COMPLETED', 'CANCELED', 'DRAFT'));

ALTER TABLE levelfiles
DROP COLUMN file_type;

ALTER TABLE levels
DROP CONSTRAINT fk_levels_event_id;

ALTER TABLE levels
DROP COLUMN event_id;

ALTER TABLE levels
ADD COLUMN hackathon_id UUID NOT NULL;

ALTER TABLE levels
ADD CONSTRAINT fk_levels_hackathon_id
FOREIGN KEY (hackathon_id)
REFERENCES hackathon(hackathon_id);

ALTER TABLE solverversion
DROP CONSTRAINT fk_solverversion_event_id;

ALTER TABLE solverversion
DROP COLUMN event_id;

ALTER TABLE solverversion
ADD COLUMN hackathon_id UUID NOT NULL;

ALTER TABLE solverversion
ADD CONSTRAINT fk_solverversion_hackathon_id
FOREIGN KEY (hackathon_id)
REFERENCES hackathon(hackathon_id);