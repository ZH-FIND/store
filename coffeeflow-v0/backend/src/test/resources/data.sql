-- 12 条历史订单：仅订单头，商品信息在 order_items 中（每个商品 1 杯，等价于 V0 的条目数语义）
INSERT INTO orders
    (id, store_name, customer_name, status, created_at, estimated_ready_at,
     note, phone_last4, pickup_code, pickup_date, total_amount)
VALUES
('CF-1001', '国贸店', 'Alice', 'NEW', DATEADD('MINUTE', -24, CURRENT_TIMESTAMP), DATEADD('MINUTE', -8, CURRENT_TIMESTAMP), '先做燕麦拿铁', NULL, NULL, NULL, 0.00),
('CF-1002', '望京店', '顾客2', 'IN_PROGRESS', DATEADD('MINUTE', -23, CURRENT_TIMESTAMP), DATEADD('MINUTE', -7, CURRENT_TIMESTAMP), '门店订单备注', NULL, NULL, NULL, 0.00),
('CF-1003', '国贸店', '顾客3', 'READY', DATEADD('MINUTE', -22, CURRENT_TIMESTAMP), DATEADD('MINUTE', -6, CURRENT_TIMESTAMP), '门店订单备注', NULL, NULL, NULL, 0.00),
('CF-1004', '中关村店', '顾客4', 'COMPLETED', DATEADD('MINUTE', -21, CURRENT_TIMESTAMP), DATEADD('MINUTE', 4, CURRENT_TIMESTAMP), '门店订单备注', NULL, NULL, NULL, 0.00),
('CF-1005', '三里屯店', '顾客5', 'NEW', DATEADD('MINUTE', -20, CURRENT_TIMESTAMP), DATEADD('MINUTE', -5, CURRENT_TIMESTAMP), '门店订单备注', NULL, NULL, NULL, 0.00),
('CF-1006', '望京店', '顾客6', 'IN_PROGRESS', DATEADD('MINUTE', -19, CURRENT_TIMESTAMP), DATEADD('MINUTE', 480, CURRENT_TIMESTAMP), '门店订单备注', NULL, NULL, NULL, 0.00),
('CF-1007', '中关村店', '顾客7', 'READY', DATEADD('MINUTE', -18, CURRENT_TIMESTAMP), DATEADD('MINUTE', 490, CURRENT_TIMESTAMP), '门店订单备注', NULL, NULL, NULL, 0.00),
('CF-1008', '国贸店', '顾客8', 'COMPLETED', DATEADD('MINUTE', -17, CURRENT_TIMESTAMP), DATEADD('MINUTE', 8, CURRENT_TIMESTAMP), '门店订单备注', NULL, NULL, NULL, 0.00),
('CF-1009', '三里屯店', '顾客9', 'CANCELLED', DATEADD('MINUTE', -16, CURRENT_TIMESTAMP), DATEADD('MINUTE', 9, CURRENT_TIMESTAMP), '门店订单备注', NULL, NULL, NULL, 0.00),
('CF-1010', '朝阳大悦城店', '顾客10', 'NEW', DATEADD('MINUTE', -15, CURRENT_TIMESTAMP), DATEADD('MINUTE', 500, CURRENT_TIMESTAMP), '门店订单备注', NULL, NULL, NULL, 0.00),
('CF-1011', '望京店', '顾客11', 'IN_PROGRESS', DATEADD('MINUTE', -14, CURRENT_TIMESTAMP), DATEADD('MINUTE', 510, CURRENT_TIMESTAMP), '门店订单备注', NULL, NULL, NULL, 0.00),
('CF-1012', '中关村店', '顾客12', 'READY', DATEADD('MINUTE', -13, CURRENT_TIMESTAMP), DATEADD('MINUTE', 520, CURRENT_TIMESTAMP), '门店订单备注', NULL, NULL, NULL, 0.00);

INSERT INTO order_items
    (order_id, line_no, product_code, drink_name, size, quantity, unit_price, subtotal)
VALUES
('CF-1001', 1, 'CF-BEV-001', '燕麦拿铁', '大杯', 1, 32.00, 32.00),
('CF-1002', 1, 'CF-BEV-002', '美式', '小杯', 1, 22.00, 22.00),
('CF-1002', 2, 'CF-BEV-001', '燕麦拿铁', '小杯', 1, 32.00, 32.00),
('CF-1003', 1, 'CF-BEV-003', '抹茶拿铁', '中杯', 1, 30.00, 30.00),
('CF-1003', 2, 'CF-BEV-002', '美式', '中杯', 1, 22.00, 22.00),
('CF-1003', 3, 'CF-BEV-004', '冷萃', '中杯', 1, 28.00, 28.00),
('CF-1004', 1, 'CF-BEV-004', '冷萃', '小杯', 1, 28.00, 28.00),
('CF-1005', 1, 'CF-BEV-005', '澳白', '中杯', 1, 30.00, 30.00),
('CF-1005', 2, 'CF-BEV-007', '卡布奇诺', '中杯', 1, 28.00, 28.00),
('CF-1006', 1, 'CF-BEV-006', '摩卡', '小杯', 1, 32.00, 32.00),
('CF-1006', 2, 'CF-BEV-011', '冰拿铁', '小杯', 1, 29.00, 29.00),
('CF-1006', 3, 'CF-BEV-008', '浓缩', '小杯', 1, 20.00, 20.00),
('CF-1007', 1, 'CF-BEV-007', '卡布奇诺', '中杯', 1, 28.00, 28.00),
('CF-1008', 1, 'CF-BEV-008', '浓缩', '小杯', 1, 20.00, 20.00),
('CF-1008', 2, 'CF-BEV-002', '美式', '小杯', 1, 22.00, 22.00),
('CF-1009', 1, 'CF-BEV-009', '脏脏咖啡', '中杯', 1, 34.00, 34.00),
('CF-1009', 2, 'CF-BEV-006', '摩卡', '中杯', 1, 32.00, 32.00),
('CF-1009', 3, 'CF-BEV-005', '澳白', '中杯', 1, 30.00, 30.00),
('CF-1010', 1, 'CF-BEV-010', '焦糖玛奇朵', '小杯', 1, 33.00, 33.00),
('CF-1011', 1, 'CF-BEV-011', '冰拿铁', '中杯', 1, 29.00, 29.00),
('CF-1011', 2, 'CF-BEV-014', '橙C美式', '中杯', 1, 26.00, 26.00),
('CF-1012', 1, 'CF-BEV-014', '橙C美式', '小杯', 1, 26.00, 26.00),
('CF-1012', 2, 'CF-BEV-004', '冷萃', '小杯', 1, 28.00, 28.00),
('CF-1012', 3, 'CF-BEV-001', '燕麦拿铁', '小杯', 1, 32.00, 32.00);

-- 订单总额由明细汇总，与 MySQL 迁移脚本口径一致
UPDATE orders SET total_amount = (
    SELECT SUM(subtotal) FROM order_items WHERE order_items.order_id = orders.id
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

INSERT INTO stores (store_id, store_name) VALUES
('S001', '国贸店'),
('S002', '望京店'),
('S003', '中关村店'),
('S004', '三里屯店'),
('S005', '朝阳大悦城店');

INSERT INTO store_product (store_id, product_code, available)
SELECT s.store_id, p.product_code, TRUE FROM stores s CROSS JOIN products p;
