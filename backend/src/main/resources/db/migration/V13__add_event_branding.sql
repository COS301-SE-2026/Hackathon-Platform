ALTER TABLE events
    ADD COLUMN IF NOT EXISTS banner_storage_key TEXT,
    ADD COLUMN IF NOT EXISTS logo_storage_key TEXT;