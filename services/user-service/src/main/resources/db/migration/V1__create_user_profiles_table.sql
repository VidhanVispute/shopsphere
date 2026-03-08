CREATE TABLE user_profiles (
    id          UUID PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    name        VARCHAR(100),
    phone       VARCHAR(20),
    avatar_url  VARCHAR(500),
    street      VARCHAR(255),
    city        VARCHAR(100),
    state       VARCHAR(100),
    pincode     VARCHAR(20),
    country     VARCHAR(100),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_profiles_email ON user_profiles(email);