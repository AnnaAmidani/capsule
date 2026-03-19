CREATE TYPE billing_cycle AS ENUM ('monthly', 'yearly');
CREATE TYPE subscription_status AS ENUM ('active', 'past_due', 'cancelled');

CREATE TABLE subscriptions (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    stripe_subscription_id   VARCHAR(255) NOT NULL UNIQUE,
    tier                     user_tier NOT NULL,
    billing_cycle            billing_cycle NOT NULL,
    status                   subscription_status NOT NULL,
    current_period_end       TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_subscriptions_user ON subscriptions (user_id);
