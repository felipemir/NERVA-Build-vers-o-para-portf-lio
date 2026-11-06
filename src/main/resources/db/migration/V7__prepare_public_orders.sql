-- V7: prepare public checkout orders
-- Links orders to the global customer account model while keeping the store-scoped
-- customer relationship used by the current administrative model.

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS customer_account_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_customer_account'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT fk_orders_customer_account
            FOREIGN KEY (customer_account_id)
            REFERENCES customer_account(id)
            ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_orders_customer_account
    ON orders (customer_account_id);
