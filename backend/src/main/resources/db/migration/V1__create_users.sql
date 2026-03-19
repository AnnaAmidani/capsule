CREATE TYPE user_tier AS ENUM ('seed', 'vessel', 'legacy');

CREATE TABLE users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(255) UNIQUE NOT NULL,
    password_hash       VARCHAR(255),
    oauth_provider      VARCHAR(50),
    oauth_subject       VARCHAR(255),
    tier                user_tier NOT NULL DEFAULT 'seed',
    stripe_customer_id  VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT oauth_or_password CHECK (
        password_hash IS NOT NULL OR (oauth_provider IS NOT NULL AND oauth_subject IS NOT NULL)
    )
);

CREATE UNIQUE INDEX idx_users_oauth ON users (oauth_provider, oauth_subject)
    WHERE oauth_provider IS NOT NULL;

CREATE UNIQUE INDEX idx_users_stripe_customer ON users (stripe_customer_id)
    WHERE stripe_customer_id IS NOT NULL;
