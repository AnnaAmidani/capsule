CREATE TABLE recipients (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    capsule_id        UUID NOT NULL REFERENCES capsules(id) ON DELETE CASCADE,
    email             VARCHAR(255) NOT NULL,
    user_id           UUID REFERENCES users(id) ON DELETE SET NULL,
    access_token      VARCHAR(512),
    token_expires_at  TIMESTAMPTZ,
    notified_at       TIMESTAMPTZ,
    delivery_error    TEXT,
    accessed_at       TIMESTAMPTZ,
    CONSTRAINT unique_recipient_per_capsule UNIQUE (capsule_id, email)
);

CREATE INDEX idx_recipients_capsule ON recipients (capsule_id);
