CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    parent_id   UUID REFERENCES categories(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_parent_id ON categories(parent_id);

-- Root categories (no parent)
INSERT INTO categories (id, name, description, parent_id) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'Electronics',  'Electronic devices and accessories', NULL),
    ('a1000000-0000-0000-0000-000000000002', 'Fashion',      'Clothing, footwear and accessories', NULL),
    ('a1000000-0000-0000-0000-000000000003', 'Home & Living', 'Furniture, decor and appliances',   NULL);

-- Child categories
INSERT INTO categories (id, name, description, parent_id) VALUES
    ('a2000000-0000-0000-0000-000000000001', 'Phones',    'Smartphones and accessories', 'a1000000-0000-0000-0000-000000000001'),
    ('a2000000-0000-0000-0000-000000000002', 'Laptops',   'Laptops and accessories',     'a1000000-0000-0000-0000-000000000001'),
    ('a2000000-0000-0000-0000-000000000003', 'Men''s Clothing', 'Clothing for men',       'a1000000-0000-0000-0000-000000000002'),
    ('a2000000-0000-0000-0000-000000000004', 'Women''s Clothing', 'Clothing for women',   'a1000000-0000-0000-0000-000000000002');