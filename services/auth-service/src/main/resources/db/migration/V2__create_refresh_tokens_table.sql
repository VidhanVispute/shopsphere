-- Refresh tokens — stateful, revocable, stored as SHA-256 hash
-- Raw token is NEVER stored. Only its hash.
-- WHY hash refresh tokens?
-- If the DB is breached, attacker gets hashes — useless without the raw token.
-- Same principle as password hashing, applied to tokens.
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL,  -- SHA-256 hash of the raw token
    family_id   UUID         NOT NULL,  -- all tokens from one login share a family_id
    expires_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT refresh_tokens_hash_unique UNIQUE (token_hash)
);

-- family_id index — used to revoke the entire family on reuse detection
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_user   ON refresh_tokens(user_id);