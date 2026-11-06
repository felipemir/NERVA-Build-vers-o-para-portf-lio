CREATE TABLE IF NOT EXISTS customer_account (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    document VARCHAR(30),
    phone VARCHAR(30),
    person_type VARCHAR(3) NOT NULL DEFAULT 'PF',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_customer_account_person_type'
    ) THEN
        ALTER TABLE customer_account
            ADD CONSTRAINT ck_customer_account_person_type CHECK (person_type IN ('PF', 'PJ'));
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_customer_account_email
    ON customer_account (LOWER(email));

CREATE UNIQUE INDEX IF NOT EXISTS ux_customer_account_document_not_null
    ON customer_account (document)
    WHERE document IS NOT NULL AND document <> '';

