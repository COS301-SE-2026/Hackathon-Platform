CREATE TABLE communication_channels (
    channel_id UUID PRIMARY KEY,
    channel_type VARCHAR(40) NOT NULL,
    event_id UUID,
    team_id UUID,
    direct_key VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_communication_channel_event
        FOREIGN KEY (event_id)
        REFERENCES events(event_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_communication_channel_team
        FOREIGN KEY (team_id)
        REFERENCES teams(team_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_communication_channel_type
        CHECK (
            channel_type IN (
                'EVENT_ANNOUNCEMENT',
                'TEAM',
                'DIRECT'
            )
        ),
    
    CONSTRAINT chk_communication_channel_target
        CHECK (
            (
                channel_type = 'EVENT_ANNOUNCEMENT'
                AND event_id IS NOT NULL
                AND team_id IS NULL
                AND direct_key IS NULL
            )
            OR 
            (
                channel_type = 'TEAM'
                AND team_id IS NOT NULL
                AND direct_key IS NULL
            )
            OR
            (
                channel_type = 'DIRECT'
                AND team_id IS NULL
                AND direct_key IS NOT NULL
            )
        )
);

CREATE UNIQUE INDEX uq_event_announcement_channel
    ON communication_channels(event_id)
    WHERE channel_type = 'EVENT_ANNOUNCEMENT';

CREATE UNIQUE INDEX uq_team_channel
    ON communication_channels(team_id)
    WHERE channel_type = 'TEAM';

CREATE UNIQUE INDEX uq_direct_channel
    ON communication_channels(direct_key)
    WHERE channel_type = 'DIRECT';

CREATE TABLE communication_messages (
    message_id UUID PRIMARY KEY,
    channel_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    title VARCHAR(150),
    body TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_communication_message_channel
        FOREIGN KEY (channel_id)
        REFERENCES communication_channels(channel_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_communication_message_user
        FOREIGN KEY (created_by_user_id)
        REFERENCES users(user_id)
        ON DELETE RESTRICT,
    
    CONSTRAINT chk_communication_message_severity
        CHECK (severity IN ('INFO', 'IMPORTANT', 'URGENT'))
);

CREATE INDEX idx_communication_messages_channel_created 
    ON communication_messages(channel_id, created_at DESC);

CREATE TABLE announcement_email_deliveries (
    message_id UUID NOT NULL,
    user_id UUID NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    delivery_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (message_id, user_id),

    CONSTRAINT fk_announcement_delivery_message
        FOREIGN KEY (message_id)
        REFERENCES communication_messages(message_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_announcement_delivery_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,
    
    CONSTRAINT chk_announcement_delivery_status
        CHECK (
            delivery_status IN (
                'PENDING',
                'PROCESSING',
                'SENT',
                'FAILED'
            )
        )
);