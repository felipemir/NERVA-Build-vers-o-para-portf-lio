-- V4: MVP schema hardening
-- Objetivo:
-- 1) Fechar lacunas de integridade para operar um MVP comercial
-- 2) Reforcar relacionamentos multi-tenant por store_id
-- 3) Preparar base para painel administrativo (catalogo, clientes, pedidos, auditoria)

-- =========================
-- 1) CATEGORIAS
-- =========================
CREATE TABLE IF NOT EXISTS category (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES store(id),
    name VARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_category_store_slug
    ON category (store_id, slug);

CREATE INDEX IF NOT EXISTS ix_category_store_active
    ON category (store_id, active);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_category_store_id_id'
    ) THEN
        ALTER TABLE category
            ADD CONSTRAINT uq_category_store_id_id UNIQUE (store_id, id);
    END IF;
END $$;

-- =========================
-- 2) USERS (auth basico do MVP)
-- =========================
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE users
SET role = 'STORE_ADMIN'
WHERE role = 'ADMIN';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_users_role'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT ck_users_role CHECK (
                role IN (
                    'SUPER_ADMIN',
                    'STORE_OWNER',
                    'STORE_ADMIN',
                    'STORE_OPERATOR',
                    'CUSTOMER_B2B',
                    'CUSTOMER',
                    'SELLER',
                    'STOCK_MANAGER'
                )
            );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_users_store_id_id'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT uq_users_store_id_id UNIQUE (store_id, id);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'users_email_key'
    ) THEN
        ALTER TABLE users DROP CONSTRAINT users_email_key;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_users_store_email'
    ) THEN
        ALTER TABLE users
            ADD CONSTRAINT uq_users_store_email UNIQUE (store_id, email);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_users_store_role_active
    ON users (store_id, role, active);

-- =========================
-- 3) CUSTOMER
-- =========================
ALTER TABLE customer
    ADD COLUMN IF NOT EXISTS person_type VARCHAR(3) NOT NULL DEFAULT 'PF',
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_customer_person_type'
    ) THEN
        ALTER TABLE customer
            ADD CONSTRAINT ck_customer_person_type CHECK (person_type IN ('PF', 'PJ'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_customer_store_id_id'
    ) THEN
        ALTER TABLE customer
            ADD CONSTRAINT uq_customer_store_id_id UNIQUE (store_id, id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_customer_store_document'
    ) THEN
        ALTER TABLE customer
            ADD CONSTRAINT uq_customer_store_document UNIQUE (store_id, document);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_customer_store_name
    ON customer (store_id, name);

CREATE INDEX IF NOT EXISTS ix_customer_store_active
    ON customer (store_id, active);

-- =========================
-- 4) ENDERECOS DO CLIENTE
-- =========================
CREATE TABLE IF NOT EXISTS customer_address (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES store(id),
    customer_id BIGINT NOT NULL,
    address_type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY',
    recipient_name VARCHAR(120),
    zip_code VARCHAR(10) NOT NULL,
    street VARCHAR(150) NOT NULL,
    number VARCHAR(20),
    complement VARCHAR(120),
    neighborhood VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    reference VARCHAR(255),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_customer_address_type'
    ) THEN
        ALTER TABLE customer_address
            ADD CONSTRAINT ck_customer_address_type
            CHECK (address_type IN ('DELIVERY', 'BILLING', 'OTHER'));
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_customer_address_customer_store'
    ) THEN
        ALTER TABLE customer_address
            ADD CONSTRAINT fk_customer_address_customer_store
            FOREIGN KEY (store_id, customer_id)
            REFERENCES customer (store_id, id)
            ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_customer_address_store_customer
    ON customer_address (store_id, customer_id);

CREATE INDEX IF NOT EXISTS ix_customer_address_store_primary
    ON customer_address (store_id, customer_id, is_primary);

-- =========================
-- 5) PRODUCT (catalogo MVP)
-- =========================
ALTER TABLE product
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS category_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_product_store_id_id'
    ) THEN
        ALTER TABLE product
            ADD CONSTRAINT uq_product_store_id_id UNIQUE (store_id, id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_product_price_non_negative'
    ) THEN
        ALTER TABLE product
            ADD CONSTRAINT ck_product_price_non_negative CHECK (price >= 0);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_product_stock_non_negative'
    ) THEN
        ALTER TABLE product
            ADD CONSTRAINT ck_product_stock_non_negative CHECK (stock_quantity >= 0);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_product_weight_non_negative'
    ) THEN
        ALTER TABLE product
            ADD CONSTRAINT ck_product_weight_non_negative CHECK (weight_kg IS NULL OR weight_kg >= 0);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_product_category'
    ) THEN
        ALTER TABLE product DROP CONSTRAINT fk_product_category;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_product_category_store'
    ) THEN
        ALTER TABLE product
            ADD CONSTRAINT fk_product_category_store
            FOREIGN KEY (store_id, category_id)
            REFERENCES category (store_id, id)
            ON DELETE RESTRICT;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_product_store_sku_not_null
    ON product (store_id, sku)
    WHERE sku IS NOT NULL AND sku <> '';

CREATE INDEX IF NOT EXISTS ix_product_store_active
    ON product (store_id, active);

CREATE INDEX IF NOT EXISTS ix_product_store_category
    ON product (store_id, category_id);

-- =========================
-- 6) ORDERS (fluxo comercial MVP)
-- =========================
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS order_number VARCHAR(40),
    ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20),
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS updated_by_user_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_orders_store_id_id'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT uq_orders_store_id_id UNIQUE (store_id, id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_orders_total_amount_non_negative'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT ck_orders_total_amount_non_negative CHECK (total_amount >= 0);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_orders_status'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT ck_orders_status CHECK (
                status IN (
                    'PENDING',
                    'PAID',
                    'SHIPPED',
                    'CANCELED',
                    'CONFIRMED',
                    'SEPARATING',
                    'DELIVERED'
                )
            );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_orders_payment_status'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT ck_orders_payment_status CHECK (
                payment_status IN ('PENDING', 'AUTHORIZED', 'PAID', 'REFUNDED', 'FAILED', 'CANCELED')
            );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_orders_payment_method'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT ck_orders_payment_method CHECK (
                payment_method IS NULL OR payment_method IN ('PIX', 'CARD', 'BOLETO', 'CASH', 'INVOICE')
            );
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'orders_customer_id_fkey'
    ) THEN
        ALTER TABLE orders DROP CONSTRAINT orders_customer_id_fkey;
    END IF;

    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'orders_user_id_fkey'
    ) THEN
        ALTER TABLE orders DROP CONSTRAINT orders_user_id_fkey;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_customer_store'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT fk_orders_customer_store
            FOREIGN KEY (store_id, customer_id)
            REFERENCES customer (store_id, id)
            ON DELETE RESTRICT;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_user_store'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT fk_orders_user_store
            FOREIGN KEY (store_id, user_id)
            REFERENCES users (store_id, id)
            ON DELETE RESTRICT;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_updated_by_user_store'
    ) THEN
        ALTER TABLE orders
            ADD CONSTRAINT fk_orders_updated_by_user_store
            FOREIGN KEY (store_id, updated_by_user_id)
            REFERENCES users (store_id, id)
            ON DELETE RESTRICT;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_orders_store_order_number
    ON orders (store_id, order_number)
    WHERE order_number IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_orders_store_status_created_at
    ON orders (store_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_orders_store_payment_status
    ON orders (store_id, payment_status);

CREATE INDEX IF NOT EXISTS ix_orders_store_customer
    ON orders (store_id, customer_id);

-- =========================
-- 7) ORDER_ITEMS (integridade forte)
-- =========================
ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS store_id BIGINT,
    ADD COLUMN IF NOT EXISTS sku_snapshot VARCHAR(50),
    ADD COLUMN IF NOT EXISTS name_snapshot VARCHAR(200),
    ADD COLUMN IF NOT EXISTS unit_measure VARCHAR(20) NOT NULL DEFAULT 'UN';

UPDATE order_items oi
SET store_id = o.store_id
FROM orders o
WHERE oi.order_id = o.id
  AND oi.store_id IS NULL;

ALTER TABLE order_items
    ALTER COLUMN store_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_order_items_quantity_positive'
    ) THEN
        ALTER TABLE order_items
            ADD CONSTRAINT ck_order_items_quantity_positive CHECK (quantity > 0);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_order_items_price_non_negative'
    ) THEN
        ALTER TABLE order_items
            ADD CONSTRAINT ck_order_items_price_non_negative CHECK (unit_price >= 0 AND subtotal >= 0);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'order_items_order_id_fkey'
    ) THEN
        ALTER TABLE order_items DROP CONSTRAINT order_items_order_id_fkey;
    END IF;

    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'order_items_product_id_fkey'
    ) THEN
        ALTER TABLE order_items DROP CONSTRAINT order_items_product_id_fkey;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_order_items_order_store'
    ) THEN
        ALTER TABLE order_items
            ADD CONSTRAINT fk_order_items_order_store
            FOREIGN KEY (store_id, order_id)
            REFERENCES orders (store_id, id)
            ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_order_items_product_store'
    ) THEN
        ALTER TABLE order_items
            ADD CONSTRAINT fk_order_items_product_store
            FOREIGN KEY (store_id, product_id)
            REFERENCES product (store_id, id)
            ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_order_items_store_order
    ON order_items (store_id, order_id);

CREATE INDEX IF NOT EXISTS ix_order_items_store_product
    ON order_items (store_id, product_id);

-- =========================
-- 8) AUDITORIA BASICA DO PAINEL ADMIN
-- =========================
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    store_id BIGINT NOT NULL REFERENCES store(id),
    user_id BIGINT,
    entity_type VARCHAR(60) NOT NULL,
    entity_id BIGINT,
    action VARCHAR(20) NOT NULL,
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_audit_log_action'
    ) THEN
        ALTER TABLE audit_log
            ADD CONSTRAINT ck_audit_log_action
            CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'STATUS_CHANGE', 'LOGIN', 'LOGOUT'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_audit_log_store_created_at
    ON audit_log (store_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_audit_log_store_entity
    ON audit_log (store_id, entity_type, entity_id);
