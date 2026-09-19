DROP TABLE IF EXISTS orders;
CREATE TABLE orders (
    id VARCHAR(32) PRIMARY KEY,
    store_name VARCHAR(64) NOT NULL,
    customer_name VARCHAR(64) NOT NULL,
    product_code VARCHAR(32) NOT NULL,
    drink_name VARCHAR(64) NOT NULL,
    size VARCHAR(16) NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    estimated_ready_at TIMESTAMP NOT NULL,
    items VARCHAR(255) NOT NULL,
    note VARCHAR(255) NOT NULL
);
