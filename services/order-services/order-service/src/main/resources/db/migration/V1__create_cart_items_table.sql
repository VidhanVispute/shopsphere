CREATE TABLE cart_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL,
    product_id    UUID NOT NULL,
    vendor_id     UUID NOT NULL,
    product_name  VARCHAR(255) NOT NULL,
    product_price NUMERIC(12, 2) NOT NULL,
    quantity      INTEGER NOT NULL CHECK (quantity > 0),
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_cart_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_cart_items_user_id ON cart_items(user_id);