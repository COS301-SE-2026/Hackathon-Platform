ALTER TABLE levelfiles
    ADD COLUMN IF NOT EXISTS file_size BIGINT,
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(255),
    ADD COLUMN IF NOT EXISTS file_type VARCHAR(20) NOT NULL;

ALTER TABLE solverversion
    ADD COLUMN IF NOT EXISTS version_number INT,
    ADD COLUMN IF NOT EXISTS file_name TEXT,
    ADD COLUMN IF NOT EXISTS file_size BIGINT,
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(255);

ALTER TABLE submissions
    ADD COLUMN IF NOT EXISTS output_file_name TEXT,
    ADD COLUMN IF NOT EXISTS source_file_name TEXT,
    ADD COLUMN IF NOT EXISTS output_file_size BIGINT,
    ADD COLUMN IF NOT EXISTS source_file_size BIGINT,
    ADD COLUMN IF NOT EXISTS output_content_type VARCHAR(255),
    ADD COLUMN IF NOT EXISTS source_content_type VARCHAR(255);