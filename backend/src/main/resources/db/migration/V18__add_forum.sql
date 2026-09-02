CREATE TABLE forum_posts (
    post_id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    author_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    deleted_by_user_id UUID,

    CONSTRAINT fk_forum_posts_event
        FOREIGN KEY (event_id)
        REFERENCES events(event_id),

    CONSTRAINT fk_forum_posts_author
        FOREIGN KEY (author_id)
        REFERENCES users(user_id),

    CONSTRAINT fk_forum_posts_deleted_by
        FOREIGN KEY (deleted_by_user_id)
        REFERENCES users(user_id),
    
    CONSTRAINT chk_forum_posts_title
        CHECK (LENGTH(TRIM(title)) > 0),

    CONSTRAINT chk_forum_posts_body
        CHECK (LENGTH(TRIM(body)) > 0)
);

CREATE TABLE forum_comments (
    comment_id UUID PRIMARY KEY,
    post_id UUID NOT NULL,
    author_id UUID NOT NULL,
    body TEXT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    deleted_by_user_id UUID,

    CONSTRAINT fk_forum_comments_post
        FOREIGN KEY (post_id)
        REFERENCES forum_posts(post_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_forum_comments_author
        FOREIGN KEY (author_id)
        REFERENCES users(user_id),

    CONSTRAINT fk_forum_comments_deleted_by
        FOREIGN KEY (deleted_by_user_id)
        REFERENCES users(user_id),

    CONSTRAINT chk_forum_comments_body
        CHECK (LENGTH(TRIM(body)) > 0)
);

CREATE INDEX idx_forum_posts_event_created
    ON forum_posts(event_id, created_at DESC);

CREATE INDEX idx_forum_comments_post_created
    ON forum_comments(post_id, created_at);

CREATE INDEX idx_forum_posts_author
    ON forum_posts(author_id);

CREATE INDEX idx_forum_comments_author
    ON forum_comments(author_id);