DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS store_product;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS stores;
DROP TABLE IF EXISTS orders;

CREATE TABLE orders (
    id VARCHAR(32) PRIMARY KEY,
    store_name VARCHAR(64) NOT NULL,
    customer_name VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    estimated_ready_at TIMESTAMP NOT NULL,
    note VARCHAR(255) NOT NULL,
    phone_last4 VARCHAR(4),
    pickup_code VARCHAR(8),
    pickup_date DATE,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00
);

-- 取餐码当日唯一（允许 pickup_code / pickup_date 均为 NULL 的历史订单并存）
CREATE UNIQUE INDEX uk_orders_pickup ON orders (pickup_code, pickup_date);

CREATE TABLE products (
    product_code VARCHAR(32) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    price DECIMAL(10, 2) NOT NULL
);

CREATE TABLE stores (
    store_id VARCHAR(16) PRIMARY KEY,
    store_name VARCHAR(64) NOT NULL
);

CREATE TABLE store_product (
    store_id VARCHAR(16) NOT NULL,
    product_code VARCHAR(32) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (store_id, product_code)
);

CREATE TABLE order_items (
    order_id VARCHAR(32) NOT NULL,
    line_no INT NOT NULL,
    product_code VARCHAR(32) NOT NULL,
    drink_name VARCHAR(64) NOT NULL,
    size VARCHAR(16) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (order_id, line_no)
);
