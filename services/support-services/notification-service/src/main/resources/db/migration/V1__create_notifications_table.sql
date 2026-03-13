CREATE TABLE notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID,
    user_id         UUID,
    user_email      VARCHAR(255) NOT NULL,
    event_type      VARCHAR(50)  NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    body            TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'SENT',
    error_message   TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);