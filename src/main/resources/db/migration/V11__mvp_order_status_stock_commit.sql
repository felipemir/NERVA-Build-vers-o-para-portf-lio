ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS stock_committed BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE orders
SET stock_committed = FALSE
WHERE stock_committed IS NULL;
