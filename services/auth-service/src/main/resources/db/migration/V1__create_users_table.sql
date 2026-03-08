-- Users table — core identity record
-- Auth Service owns this. User Service has a separate profile table.
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,  -- bcrypt hash, never plaintext
    role        VARCHAR(20)  NOT NULL,  -- CUSTOMER, VENDOR, ADMIN
    status      VARCHAR(30)  NOT NULL,  -- PENDING_VERIFICATION, ACTIVE, SUSPENDED, BANNED
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT users_email_unique UNIQUE (email),
    CONSTRAINT users_role_check   CHECK (role IN ('CUSTOMER', 'VENDOR', 'ADMIN')),
    CONSTRAINT users_status_check CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'BANNED'))
);

CREATE INDEX idx_users_email ON users(email);