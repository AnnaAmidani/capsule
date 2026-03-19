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
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT unique_recipient_per_capsule UNIQUE (capsule_id, email),
    CONSTRAINT token_expiry_required CHECK (access_token IS NULL OR token_expires_at IS NOT NULL)
);

CREATE INDEX idx_recipients_capsule ON recipients (capsule_id);
