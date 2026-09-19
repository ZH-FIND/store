USE coffeeflow;

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

INSERT INTO orders
    (id, store_name, customer_name, product_code, drink_name, size, quantity, status,
     created_at, estimated_ready_at, items, note)
VALUES
('CF-1001', '国贸店', 'Alice', 'CF-BEV-001', '燕麦拿铁', '大杯', 1, 'NEW', CURRENT_TIMESTAMP - INTERVAL 24 MINUTE, CURRENT_TIMESTAMP - INTERVAL 8 MINUTE, '燕麦拿铁', '先做燕麦拿铁'),
('CF-1002', '望京店', '顾客2', 'CF-BEV-002', '美式', '小杯', 2, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL 23 MINUTE, CURRENT_TIMESTAMP - INTERVAL 7 MINUTE, '美式|燕麦拿铁', '门店订单备注'),
('CF-1003', '国贸店', '顾客3', 'CF-BEV-003', '抹茶拿铁', '中杯', 3, 'READY', CURRENT_TIMESTAMP - INTERVAL 22 MINUTE, CURRENT_TIMESTAMP - INTERVAL 6 MINUTE, '抹茶拿铁|美式|冷萃', '门店订单备注'),
('CF-1004', '中关村店', '顾客4', 'CF-BEV-004', '冷萃', '小杯', 1, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL 21 MINUTE, CURRENT_TIMESTAMP + INTERVAL 4 MINUTE, '冷萃', '门店订单备注'),
('CF-1005', '三里屯店', '顾客5', 'CF-BEV-005', '澳白', '中杯', 2, 'NEW', CURRENT_TIMESTAMP - INTERVAL 20 MINUTE, CURRENT_TIMESTAMP - INTERVAL 5 MINUTE, '澳白|卡布奇诺', '门店订单备注'),
('CF-1006', '望京店', '顾客6', 'CF-BEV-006', '摩卡', '小杯', 3, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL 19 MINUTE, CURRENT_TIMESTAMP + INTERVAL 480 MINUTE, '摩卡|冰拿铁|浓缩', '门店订单备注'),
('CF-1007', '中关村店', '顾客7', 'CF-BEV-007', '卡布奇诺', '中杯', 1, 'READY', CURRENT_TIMESTAMP - INTERVAL 18 MINUTE, CURRENT_TIMESTAMP + INTERVAL 490 MINUTE, '卡布奇诺', '门店订单备注'),
('CF-1008', '国贸店', '顾客8', 'CF-BEV-008', '浓缩', '小杯', 2, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL 17 MINUTE, CURRENT_TIMESTAMP + INTERVAL 8 MINUTE, '浓缩|美式', '门店订单备注'),
('CF-1009', '三里屯店', '顾客9', 'CF-BEV-009', '脏脏咖啡', '中杯', 3, 'CANCELLED', CURRENT_TIMESTAMP - INTERVAL 16 MINUTE, CURRENT_TIMESTAMP + INTERVAL 9 MINUTE, '脏脏咖啡|摩卡|澳白', '门店订单备注'),
('CF-1010', '朝阳大悦城店', '顾客10', 'CF-BEV-010', '焦糖玛奇朵', '小杯', 1, 'NEW', CURRENT_TIMESTAMP - INTERVAL 15 MINUTE, CURRENT_TIMESTAMP + INTERVAL 500 MINUTE, '焦糖玛奇朵', '门店订单备注'),
('CF-1011', '望京店', '顾客11', 'CF-BEV-011', '冰拿铁', '中杯', 2, 'IN_PROGRESS', CURRENT_TIMESTAMP - INTERVAL 14 MINUTE, CURRENT_TIMESTAMP + INTERVAL 510 MINUTE, '冰拿铁|橙C美式', '门店订单备注'),
('CF-1012', '中关村店', '顾客12', 'CF-BEV-014', '橙C美式', '小杯', 3, 'READY', CURRENT_TIMESTAMP - INTERVAL 13 MINUTE, CURRENT_TIMESTAMP + INTERVAL 520 MINUTE, '橙C美式|冷萃|燕麦拿铁', '门店订单备注');
