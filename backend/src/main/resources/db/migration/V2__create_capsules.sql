CREATE TYPE capsule_state AS ENUM ('draft', 'sealed', 'accessible', 'archived');
CREATE TYPE capsule_visibility AS ENUM ('public', 'private');

CREATE TABLE capsules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id            UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title               VARCHAR(255) NOT NULL,
    state               capsule_state NOT NULL DEFAULT 'draft',
    visibility          capsule_visibility NOT NULL DEFAULT 'public',
    open_at             TIMESTAMPTZ NOT NULL,
    encryption_key_hint VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_capsules_owner ON capsules (owner_id);
CREATE INDEX idx_capsules_delivery ON capsules (state, open_at)
    WHERE state = 'sealed';
