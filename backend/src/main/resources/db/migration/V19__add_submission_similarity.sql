-- Plagiarism detection
CREATE TABLE IF NOT EXISTS submission_similarity (
    id                  BIGSERIAL PRIMARY KEY,
    event_id            UUID        NOT NULL REFERENCES events(event_id),
    level_id            SMALLINT    NOT NULL,
    submission_id_a     BIGINT      NOT NULL REFERENCES submissions(id),
    submission_id_b     BIGINT      NOT NULL REFERENCES submissions(id),
    team_id_a           UUID        NOT NULL REFERENCES teams(team_id),
    team_id_b           UUID        NOT NULL REFERENCES teams(team_id),
    structural_score    NUMERIC(5,4) NOT NULL,
    embedding_score      NUMERIC(5,4),
    combined_score       NUMERIC(5,4) NOT NULL,
    matched_kgram_count   INTEGER NOT NULL DEFAULT 0,
    flagged             BOOLEAN     NOT NULL DEFAULT FALSE,
    run_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_submission_similarity_pair UNIQUE (submission_id_a, submission_id_b)
);

CREATE INDEX IF NOT EXISTS idx_submission_similarity_event_level
    ON submission_similarity (event_id, level_id);

CREATE INDEX IF NOT EXISTS idx_submission_similarity_flagged
    ON submission_similarity (event_id, level_id, flagged);

CREATE TABLE IF NOT EXISTS plagiarism_run (
    id            BIGSERIAL PRIMARY KEY,
    event_id      UUID        NOT NULL REFERENCES events(event_id),
    level_id      SMALLINT,
    top_n         INTEGER     NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    pairs_compared INTEGER    NOT NULL DEFAULT 0,
    pairs_flagged  INTEGER    NOT NULL DEFAULT 0,
    error_message TEXT,
    requested_by  UUID,
    requested_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at  TIMESTAMPTZ
);
