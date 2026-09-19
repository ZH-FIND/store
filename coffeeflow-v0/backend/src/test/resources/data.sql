INSERT INTO orders
    (id, store_name, customer_name, product_code, drink_name, size, quantity, status,
     created_at, estimated_ready_at, items, note)
VALUES
('CF-1001', '国贸店', 'Alice', 'CF-BEV-001', '燕麦拿铁', '大杯', 1, 'NEW', DATEADD('MINUTE', -24, CURRENT_TIMESTAMP), DATEADD('MINUTE', -8, CURRENT_TIMESTAMP), '燕麦拿铁', '先做燕麦拿铁'),
('CF-1002', '望京店', '顾客2', 'CF-BEV-002', '美式', '小杯', 2, 'IN_PROGRESS', DATEADD('MINUTE', -23, CURRENT_TIMESTAMP), DATEADD('MINUTE', -7, CURRENT_TIMESTAMP), '美式|燕麦拿铁', '门店订单备注'),
('CF-1003', '国贸店', '顾客3', 'CF-BEV-003', '抹茶拿铁', '中杯', 3, 'READY', DATEADD('MINUTE', -22, CURRENT_TIMESTAMP), DATEADD('MINUTE', -6, CURRENT_TIMESTAMP), '抹茶拿铁|美式|冷萃', '门店订单备注'),
('CF-1004', '中关村店', '顾客4', 'CF-BEV-004', '冷萃', '小杯', 1, 'COMPLETED', DATEADD('MINUTE', -21, CURRENT_TIMESTAMP), DATEADD('MINUTE', 4, CURRENT_TIMESTAMP), '冷萃', '门店订单备注'),
('CF-1005', '三里屯店', '顾客5', 'CF-BEV-005', '澳白', '中杯', 2, 'NEW', DATEADD('MINUTE', -20, CURRENT_TIMESTAMP), DATEADD('MINUTE', -5, CURRENT_TIMESTAMP), '澳白|卡布奇诺', '门店订单备注'),
('CF-1006', '望京店', '顾客6', 'CF-BEV-006', '摩卡', '小杯', 3, 'IN_PROGRESS', DATEADD('MINUTE', -19, CURRENT_TIMESTAMP), DATEADD('MINUTE', 480, CURRENT_TIMESTAMP), '摩卡|冰拿铁|浓缩', '门店订单备注'),
('CF-1007', '中关村店', '顾客7', 'CF-BEV-007', '卡布奇诺', '中杯', 1, 'READY', DATEADD('MINUTE', -18, CURRENT_TIMESTAMP), DATEADD('MINUTE', 490, CURRENT_TIMESTAMP), '卡布奇诺', '门店订单备注'),
('CF-1008', '国贸店', '顾客8', 'CF-BEV-008', '浓缩', '小杯', 2, 'COMPLETED', DATEADD('MINUTE', -17, CURRENT_TIMESTAMP), DATEADD('MINUTE', 8, CURRENT_TIMESTAMP), '浓缩|美式', '门店订单备注'),
('CF-1009', '三里屯店', '顾客9', 'CF-BEV-009', '脏脏咖啡', '中杯', 3, 'CANCELLED', DATEADD('MINUTE', -16, CURRENT_TIMESTAMP), DATEADD('MINUTE', 9, CURRENT_TIMESTAMP), '脏脏咖啡|摩卡|澳白', '门店订单备注'),
('CF-1010', '朝阳大悦城店', '顾客10', 'CF-BEV-010', '焦糖玛奇朵', '小杯', 1, 'NEW', DATEADD('MINUTE', -15, CURRENT_TIMESTAMP), DATEADD('MINUTE', 500, CURRENT_TIMESTAMP), '焦糖玛奇朵', '门店订单备注'),
('CF-1011', '望京店', '顾客11', 'CF-BEV-011', '冰拿铁', '中杯', 2, 'IN_PROGRESS', DATEADD('MINUTE', -14, CURRENT_TIMESTAMP), DATEADD('MINUTE', 510, CURRENT_TIMESTAMP), '冰拿铁|橙C美式', '门店订单备注'),
('CF-1012', '中关村店', '顾客12', 'CF-BEV-014', '橙C美式', '小杯', 3, 'READY', DATEADD('MINUTE', -13, CURRENT_TIMESTAMP), DATEADD('MINUTE', 520, CURRENT_TIMESTAMP), '橙C美式|冷萃|燕麦拿铁', '门店订单备注');
