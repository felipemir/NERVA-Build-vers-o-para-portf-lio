-- 1. Tabela de Lojas (Tenants)
CREATE TABLE store (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(200) NOT NULL,
                       subdomain VARCHAR(50) NOT NULL UNIQUE, -- ex: "obraja"
                       active BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela de Produtos (Com suporte a frete/peso)
CREATE TABLE product (
                         id BIGSERIAL PRIMARY KEY,
                         store_id BIGINT NOT NULL REFERENCES store(id), -- Multi-tenancy obrigatório

                         sku VARCHAR(50), -- Código de barras/interno
                         name VARCHAR(200) NOT NULL,
                         description TEXT,
                         price DECIMAL(10, 2) NOT NULL,

    -- Logística (CRUCIAL para o Frete Híbrido)
                         weight_kg DECIMAL(10, 3),      -- Peso
                         stock_quantity DECIMAL(10, 2) NOT NULL DEFAULT 0,

                         active BOOLEAN DEFAULT TRUE,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- INSERÇÃO DA NOSSA PRIMEIRA LOJA REAL
-- O ID será 1. O subdomínio 'obraja' será usado para identificar a loja na URL.
INSERT INTO store (name, subdomain, active) VALUES ('Obra Já', 'obraja', true);