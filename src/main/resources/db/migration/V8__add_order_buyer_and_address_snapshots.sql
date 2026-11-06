ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS buyer_name_snapshot VARCHAR(150),
    ADD COLUMN IF NOT EXISTS buyer_email_snapshot VARCHAR(150),
    ADD COLUMN IF NOT EXISTS buyer_phone_snapshot VARCHAR(30),
    ADD COLUMN IF NOT EXISTS buyer_document_snapshot VARCHAR(30),
    ADD COLUMN IF NOT EXISTS buyer_person_type_snapshot VARCHAR(3),
    ADD COLUMN IF NOT EXISTS delivery_zip_code_snapshot VARCHAR(10),
    ADD COLUMN IF NOT EXISTS delivery_street_snapshot VARCHAR(150),
    ADD COLUMN IF NOT EXISTS delivery_number_snapshot VARCHAR(20),
    ADD COLUMN IF NOT EXISTS delivery_neighborhood_snapshot VARCHAR(100),
    ADD COLUMN IF NOT EXISTS delivery_city_snapshot VARCHAR(100),
    ADD COLUMN IF NOT EXISTS delivery_state_snapshot VARCHAR(2);

UPDATE orders o
SET buyer_name_snapshot = COALESCE(o.buyer_name_snapshot, (SELECT ca.name FROM customer_account ca WHERE ca.id = o.customer_account_id), c.name),
    buyer_email_snapshot = COALESCE(o.buyer_email_snapshot, (SELECT ca.email FROM customer_account ca WHERE ca.id = o.customer_account_id), c.email),
    buyer_phone_snapshot = COALESCE(o.buyer_phone_snapshot, (SELECT ca.phone FROM customer_account ca WHERE ca.id = o.customer_account_id), c.phone),
    buyer_document_snapshot = COALESCE(o.buyer_document_snapshot, (SELECT ca.document FROM customer_account ca WHERE ca.id = o.customer_account_id), c.document),
    buyer_person_type_snapshot = COALESCE(o.buyer_person_type_snapshot, (SELECT ca.person_type FROM customer_account ca WHERE ca.id = o.customer_account_id), c.person_type),
    delivery_zip_code_snapshot = COALESCE(o.delivery_zip_code_snapshot, c.zip_code),
    delivery_street_snapshot = COALESCE(o.delivery_street_snapshot, c.street),
    delivery_number_snapshot = COALESCE(o.delivery_number_snapshot, c.number),
    delivery_neighborhood_snapshot = COALESCE(o.delivery_neighborhood_snapshot, c.neighborhood),
    delivery_city_snapshot = COALESCE(o.delivery_city_snapshot, c.city),
    delivery_state_snapshot = COALESCE(o.delivery_state_snapshot, c.state)
FROM customer c
WHERE o.store_id = c.store_id
  AND o.customer_id = c.id;
