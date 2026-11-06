ALTER TABLE store
    ADD COLUMN IF NOT EXISTS marketplace_visible BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE store
SET marketplace_visible = FALSE
WHERE subdomain = 'obraja';
