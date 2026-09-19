SET NAMES utf8mb4;

USE coffeeflow;

-- ============================================================
-- V1 增量迁移：线上下单 + 门店商品可售
-- 顺序：建新表 → 派生明细 → 改造 orders → 建索引
-- 先派生明细再删旧列，保证数据不丢
-- ============================================================

-- ---------- 1. 商品目录 ----------
CREATE TABLE products (
    product_code VARCHAR(32) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    price DECIMAL(10, 2) NOT NULL
);

INSERT INTO products (product_code, name, price) VALUES
('CF-BEV-001', '燕麦拿铁', 32.00),
('CF-BEV-002', '美式', 22.00),
('CF-BEV-003', '抹茶拿铁', 30.00),
('CF-BEV-004', '冷萃', 28.00),
('CF-BEV-005', '澳白', 30.00),
('CF-BEV-006', '摩卡', 32.00),
('CF-BEV-007', '卡布奇诺', 28.00),
('CF-BEV-008', '浓缩', 20.00),
('CF-BEV-009', '脏脏咖啡', 34.00),
('CF-BEV-010', '焦糖玛奇朵', 33.00),
('CF-BEV-011', '冰拿铁', 29.00),
('CF-BEV-012', '拿铁', 28.00),
('CF-BEV-013', '生椰拿铁', 31.00),
('CF-BEV-014', '橙C美式', 26.00);

-- ---------- 2. 门店（只读种子） ----------
CREATE TABLE stores (
    store_id VARCHAR(16) PRIMARY KEY,
    store_name VARCHAR(64) NOT NULL
);

INSERT INTO stores (store_id, store_name) VALUES
('S001', '国贸店'),
('S002', '望京店'),
('S003', '中关村店'),
('S004', '三里屯店'),
('S005', '朝阳大悦城店');

-- ---------- 3. 门店 × 商品可售关系 ----------
CREATE TABLE store_product (
    store_id VARCHAR(16) NOT NULL,
    product_code VARCHAR(32) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (store_id, product_code)
);

INSERT INTO store_product (store_id, product_code, available)
SELECT s.store_id, p.product_code, TRUE
FROM stores s
CROSS JOIN products p;

-- ---------- 4. 订单明细 ----------
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

-- 由 V0 orders.items 按 '|' 拆分派生明细：每个商品 1 杯（等价于 V0 的条目数语义）
INSERT INTO order_items
    (order_id, line_no, product_code, drink_name, size, quantity, unit_price, subtotal)
SELECT o.id,
       n.n + 1,
       p.product_code,
       p.name,
       o.size,
       1,
       p.price,
       p.price
FROM orders o
JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2) n
    ON n.n < 1 + (LENGTH(o.items) - LENGTH(REPLACE(o.items, '|', '')))
JOIN products p
    ON p.name = SUBSTRING_INDEX(SUBSTRING_INDEX(o.items, '|', n.n + 1), '|', -1);

-- ---------- 5. 改造 orders：加新列并回填金额 ----------
ALTER TABLE orders
    ADD COLUMN phone_last4 VARCHAR(4) NULL,
    ADD COLUMN pickup_code VARCHAR(8) NULL,
    ADD COLUMN pickup_date DATE NULL,
    ADD COLUMN total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00;

UPDATE orders o
JOIN (SELECT order_id, SUM(subtotal) AS amount FROM order_items GROUP BY order_id) t
    ON t.order_id = o.id
SET o.total_amount = t.amount;

-- ---------- 6. 删除 orders 的旧商品字段 ----------
ALTER TABLE orders
    DROP COLUMN product_code,
    DROP COLUMN drink_name,
    DROP COLUMN size,
    DROP COLUMN quantity,
    DROP COLUMN items;

-- ---------- 7. 取餐码当日唯一 ----------
ALTER TABLE orders
    ADD UNIQUE KEY uk_orders_pickup (pickup_code, pickup_date);
