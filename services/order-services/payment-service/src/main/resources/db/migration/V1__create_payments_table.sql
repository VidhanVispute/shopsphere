CREATE TABLE payments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID NOT NULL,
    user_id             UUID NOT NULL,
    amount              NUMERIC(19, 2) NOT NULL,
    payment_method      VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    razorpay_order_id   VARCHAR(100),
    razorpay_payment_id VARCHAR(100),
    razorpay_signature  VARCHAR(255),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_user_id  ON payments(user_id);
CREATE INDEX idx_payments_status   ON payments(status);