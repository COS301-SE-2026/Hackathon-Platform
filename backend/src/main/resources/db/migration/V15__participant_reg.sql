CREATE TABLE event_participants (
    registration_id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    user_id UUID NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_participants_event_id
        FOREIGN KEY (event_id)
        REFERENCES events(event_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_event_participants_user_d
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_event_participants_event_id_user_id
        UNIQUE (event_id, user_id)
);

CREATE INDEX idx_event_participants_user_id ON event_participants(user_id)