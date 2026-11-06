
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       store_id BIGINT NOT NULL REFERENCES store(id), -- Multi-tenant
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL, -- Senha criptografada
                       role VARCHAR(20) NOT NULL, -- ADMIN, SELLER, STOCK_MANAGER
                       active BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE customer (
                          id BIGSERIAL PRIMARY KEY,
                          store_id BIGINT NOT NULL REFERENCES store(id),
                          name VARCHAR(100) NOT NULL,
                          document VARCHAR(20), -- CPF ou CNPJ
                          email VARCHAR(100),
                          phone VARCHAR(20),


                          zip_code VARCHAR(10),
                          street VARCHAR(150),
                          number VARCHAR(10),
                          neighborhood VARCHAR(100),
                          city VARCHAR(100),
                          state VARCHAR(2),

                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        store_id BIGINT NOT NULL REFERENCES store(id),
                        customer_id BIGINT REFERENCES customer(id), -- Pode ser nulo se for venda balcão sem cadastro
                        user_id BIGINT REFERENCES users(id), -- Quem vendeu (Vendedor)

                        status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PAID, SHIPPED, CANCELED
                        total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,

                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL REFERENCES orders(id),
                             product_id BIGINT NOT NULL REFERENCES product(id),

                             quantity INTEGER NOT NULL,
                             unit_price DECIMAL(10, 2) NOT NULL, -- Preço NO MOMENTO da venda (se o produto aumentar depois, aqui não muda)
                             subtotal DECIMAL(10, 2) NOT NULL
);


INSERT INTO users (store_id, name, email, password, role)
VALUES (1, 'Admin Obra Já', 'admin@obraja.com', '123456', 'ADMIN');