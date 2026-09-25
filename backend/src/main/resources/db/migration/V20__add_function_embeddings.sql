CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS submission_function_embedding (
    id              BIGSERIAL PRIMARY KEY,
    submission_id   BIGINT       NOT NULL REFERENCES submissions(id) ON DELETE CASCADE,
    file_name       TEXT         NOT NULL,
    qualified_name  TEXT         NOT NULL,
    embedding       VECTOR(768)  NOT NULL,
    truncated       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_submission_function_embedding_submission
    ON submission_function_embedding (submission_id);
