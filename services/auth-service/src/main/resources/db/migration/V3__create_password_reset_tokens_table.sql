-- Password reset tokens — short-lived, single-use, stored as hash
CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,  -- 15 minutes from creation
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT password_reset_hash_unique UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id);