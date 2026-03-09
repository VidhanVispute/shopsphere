-- Drop the default that depends on the enum type
ALTER TABLE products
    ALTER COLUMN status DROP DEFAULT;

-- Convert the column to VARCHAR
ALTER TABLE products
    ALTER COLUMN status TYPE VARCHAR(20)
    USING status::TEXT;

-- Set a plain string default
ALTER TABLE products
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- Now safe to drop the type
DROP TYPE product_status;