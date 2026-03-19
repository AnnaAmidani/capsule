CREATE TYPE item_type AS ENUM ('text', 'image', 'video_link', 'music_link', 'keepsake');

CREATE TABLE capsule_items (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    capsule_id  UUID NOT NULL REFERENCES capsules(id) ON DELETE CASCADE,
    type        item_type NOT NULL,
    content     TEXT,
    s3_key      VARCHAR(1024),
    sort_order  INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_items_capsule ON capsule_items (capsule_id);
