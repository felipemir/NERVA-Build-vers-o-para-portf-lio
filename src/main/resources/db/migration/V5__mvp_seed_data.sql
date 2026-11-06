-- V5: MVP seed data and operational defaults
-- Objetivo:
-- 1) Popular dados iniciais para operacao do MVP
-- 2) Backfill de colunas novas adicionadas na V4
-- 3) Manter script idempotente para multiplos ambientes

-- =========================
-- 1) CATEGORIAS PADRAO POR LOJA
-- =========================
INSERT INTO category (store_id, name, slug, description, active)
SELECT
    s.id,
    c.name,
    c.slug,
    c.description,
    TRUE
FROM store s
CROSS JOIN (
    VALUES
        ('Cimento e Argamassa', 'cimento-argamassa', 'Cimentos, argamassas e rejuntes'),
        ('Areia, Pedra e Brita', 'areia-pedra-brita', 'Agregados para obra e concreto'),
        ('Blocos e Tijolos', 'blocos-tijolos', 'Alvenaria estrutural e vedacao'),
        ('Ferramentas', 'ferramentas', 'Ferramentas manuais e acessorios'),
        ('Hidraulica', 'hidraulica', 'Tubos, conexoes e registros'),
        ('Eletrica', 'eletrica', 'Cabos, disjuntores e iluminacao'),
        ('Pisos e Revestimentos', 'pisos-revestimentos', 'Ceramicas, porcelanatos e acabamentos'),
        ('Tintas e Acessorios', 'tintas-acessorios', 'Tintas, rolos e pincelaria')
) AS c(name, slug, description)
ON CONFLICT (store_id, slug)
DO UPDATE
SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

-- =========================
-- 2) BACKFILL: PRODUCT.CATEGORY_ID (a partir de product.category texto)
-- =========================
UPDATE product p
SET category_id = c.id
FROM category c
WHERE p.store_id = c.store_id
  AND p.category IS NOT NULL
  AND p.category_id IS NULL
  AND LOWER(TRIM(p.category)) = LOWER(TRIM(c.name));

-- =========================
-- 3) BACKFILL: NUMERO DE PEDIDO
-- =========================
UPDATE orders o
SET order_number = CONCAT('PED-', o.store_id, '-', LPAD(o.id::TEXT, 6, '0'))
WHERE o.order_number IS NULL;

-- =========================
-- 4) USUARIOS INICIAIS POR LOJA
-- NOTE: senhas em texto sao apenas para bootstrap do MVP;
-- migrar para hash (BCrypt) na implementacao de auth JWT.
-- =========================
INSERT INTO users (store_id, name, email, password, role, active)
SELECT
    s.id,
    CONCAT('Owner Loja ', s.id),
    CONCAT('owner+', s.id, '@nervabuild.local'),
    '123456',
    'STORE_OWNER',
    TRUE
FROM store s
WHERE NOT EXISTS (
    SELECT 1
    FROM users u
    WHERE u.store_id = s.id
      AND u.role = 'STORE_OWNER'
);

INSERT INTO users (store_id, name, email, password, role, active)
SELECT
    s.id,
    CONCAT('Operador Loja ', s.id),
    CONCAT('operador+', s.id, '@nervabuild.local'),
    '123456',
    'STORE_OPERATOR',
    TRUE
FROM store s
WHERE NOT EXISTS (
    SELECT 1
    FROM users u
    WHERE u.store_id = s.id
      AND u.role = 'STORE_OPERATOR'
);

-- =========================
-- 5) CLIENTE BALCAO PADRAO POR LOJA
-- =========================
INSERT INTO customer (
    store_id,
    name,
    document,
    email,
    phone,
    city,
    state,
    person_type,
    active
)
SELECT
    s.id,
    'Cliente Balcao',
    NULL,
    CONCAT('cliente-balcao+', s.id, '@nervabuild.local'),
    NULL,
    'Nao Informado',
    'NA',
    'PF',
    TRUE
FROM store s
WHERE NOT EXISTS (
    SELECT 1
    FROM customer c
    WHERE c.store_id = s.id
      AND LOWER(c.name) = 'cliente balcao'
);

-- =========================
-- 6) AJUSTES OPERACIONAIS
-- =========================
-- Se pedido possui user_id e updated_by_user_id nulo, sincroniza.
UPDATE orders
SET updated_by_user_id = user_id
WHERE user_id IS NOT NULL
  AND updated_by_user_id IS NULL;
