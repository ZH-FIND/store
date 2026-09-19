## 1. 数据库结构与种子

- [x] 1.1 新建 `sql/02-v1-online-ordering.sql`，首行 `SET NAMES utf8mb4;` 并 `USE coffeeflow;`
- [x] 1.2 建 `products`（`product_code` 主键、`name`、`price`），灌入 14 条种子，与 `frontend/public/drinks/CF-BEV-001~014.png` 一一对应；`name` 必须与 V0 种子中出现过的饮品名一致（燕麦拿铁、美式、抹茶拿铁、冷萃、澳白、摩卡、卡布奇诺、浓缩、脏脏咖啡、焦糖玛奇朵、冰拿铁、橙C美式等），且 `橙C美式` 必须为 `CF-BEV-014`、`卡布奇诺` 为 `CF-BEV-007`
- [x] 1.3 建 `stores` 并灌入 V0 的 5 个门店（国贸店、望京店、中关村店、三里屯店、朝阳大悦城店）
- [x] 1.4 建 `store_product(store_id, product_code, available)`，主键 `(store_id, product_code)`，为 5 × 14 组合灌入 `available = true`
- [x] 1.5 建 `order_items(order_id, line_no, product_code, drink_name, size, quantity, unit_price, subtotal)`，主键 `(order_id, line_no)`
- [x] 1.6 由旧 `orders.items` 按 `|` 拆分派生 `order_items`：每条明细数量为 1、`size` 取原订单 `size`、`product_code` 按 `drink_name` 反查 `products`、`unit_price` 取商品价、`subtotal = unit_price`
- [x] 1.7 为 `orders` 增加 `phone_last4`、`pickup_code`、`pickup_date`、`total_amount`，并按明细汇总回填 `total_amount`；`orders` 保留 `store_name` 且**不新增** `store_id`、不建到 `stores` 的外键（见 design D7）
- [x] 1.8 删除 `orders` 的 `product_code`、`drink_name`、`size`、`quantity`、`items` 五个旧列
- [x] 1.9 为 `orders` 建 `(pickup_code, pickup_date)` 唯一索引
- [x] 1.10 `docker compose down -v && docker compose up -d --build --wait`，确认初始化无报错且 12 条订单头与全部明细入库

## 2. 后端：商品与门店读侧

- [x] 2.1 新增 `Product`、`Store` 领域对象与 `ProductRepository`、`StoreRepository`
- [x] 2.2 `GET /api/v1/stores` 返回门店列表
- [x] 2.3 `GET /api/v1/stores/{storeId}/products` 返回该门店商品列表，字段含 `productCode`、`name`、`price`、`available`；门店不存在时返回 404
- [x] 2.4 `PATCH /api/v1/stores/{storeId}/products/{productCode}/availability` 停售 / 恢复，返回更新后的商品；门店或商品不存在时返回 404

## 3. 后端：订单模型改造（保 V0 契约）

- [x] 3.1 新增 `OrderItem` 领域对象，`Order` 去掉 `productCode`/`drinkName`/`size`/`quantity`/`items`，改为持有明细列表与 `pickupCode`/`pickupDate`/`phoneLast4`/`totalAmount`
- [x] 3.2 `OrderRepository` 查询改为两次：先按现有条件查订单头（保持 `ORDER BY id`），再按 `order_id IN (...)` 取明细并在 Java 按 `line_no` 分组
- [x] 3.3 `OrderRepository.save` 改为订单头与明细的协同写入（明细先删后插），保持 `PATCH /status` 只更新订单头状态
- [x] 3.4 `OrderSummaryResponse` / `OrderDetailResponse` 的字段名与结构保持不变，映射规则：`quantity` = 明细条数、`items` = 明细饮品名列表、`productCode`/`drinkName`/`size` = 第一条明细
- [x] 3.5 运行 `mvn verify`，确认 `CoffeeFlowApiTests` 与 `JdbcBaselineTests` 无需修改即全部通过

## 4. 后端：下单与取餐码

- [x] 4.1 新增请求 DTO：门店、顾客姓名、手机号后四位、明细（商品码 / 规格 / 数量）、备注
- [x] 4.2 实现订单号生成（沿用 `CF-` 前缀 + 递增序号，不与既有 12 条冲突）
- [x] 4.3 实现取餐码生成：取当日已用最大取餐码 + 1，当日首单为 `1001`；写入 `pickup_date = 当前日期`
- [x] 4.4 实现下单校验：门店存在、明细非空、数量均 ≥ 1、商品均属于该门店且在 `store_product` 中 `available = true`；违反时返回 400 并指明具体商品
- [x] 4.5 由提交的 `storeId` 反查门店名并写入订单的 `store_name`（见 design D7）
- [x] 4.6 按明细汇总 `total_amount`，每条明细写入 `unit_price` 与 `subtotal` 快照；`unit_price` 一律取 `products.price`，**不随 `size` 变化**（见 design D6）；`estimated_ready_at` 按下单时间 + 15 分钟生成
- [x] 4.7 `POST /api/v1/orders` 返回 `orderId`、`pickupCode`、`totalAmount`、`status`
- [x] 4.8 确认新订单在 `GET /api/v1/orders` 中可见且状态为 `NEW`，`quantity` 等于明细条数

## 5. 后端：取餐码查询与取消

- [x] 5.1 `GET /api/v1/orders/pickup/{pickupCode}` 仅在当日范围内匹配非空取餐码，返回明细、金额与状态；未命中返回 404
- [x] 5.2 `POST /api/v1/orders/{orderId}/cancel` 校验取餐码与 `phoneLast4` 同时匹配
- [x] 5.3 取消规则：仅 `NEW` 可取消；`IN_PROGRESS` 及之后返回 409/400；已 `CANCELLED` 重复取消返回明确错误；取消只改状态不删单
- [x] 5.4 确认取消后的订单仍出现在门店看板且状态为「已取消」

## 6. 后端：测试

- [x] 6.1 为新增能力补集成测试：门店商品列表（含不存在门店 404）、下单成功、下单含停售商品被拒、数量非法被拒、取餐码查询、取消成功、制作中取消被拒、手机号不匹配被拒
- [x] 6.2 补测试：停售只影响本门店（A 店停售后 B 店仍可下单）
- [x] 6.3 补测试：同一取餐码在不同日期可共存；跨天重置从 1001 开始
- [x] 6.4 补测试：改价后已有订单金额不变
- [x] 6.5 补测试：同一商品选不同规格（小杯 / 大杯）下单，单价与总额一致（见 design D6）
- [x] 6.6 补测试：下单后订单的 `storeName` 与所选门店一致，且看板按该门店名可筛到
- [x] 6.7 更新 `src/test/resources/schema.sql` 与 `data.sql`，使 H2 测试库结构与新模型一致，且保证既有基线断言（total=12、READY=3、IN_PROGRESS=3、overdue=4）继续成立
- [x] 6.8 `mvn verify` 全绿

## 7. 前端：Tab 与顾客点单

- [x] 7.1 `App.vue` 增加 Tab 切换「门店看板」与「顾客点单」，默认停留在门店看板
- [x] 7.2 顾客端实现门店选择（数据来自 `GET /api/v1/stores`）
- [x] 7.3 选定门店后加载商品列表，展示图片（`/drinks/{productCode}.png`）、名称、价格；停售商品可见但禁用且标记不可售
- [x] 7.4 实现购物车：同一商品加减数量、多个商品、合计金额，禁止选择停售商品
- [x] 7.5 实现下单表单（姓名、手机号后四位、备注）并提交 `POST /api/v1/orders`，成功后展示订单金额与取餐码
- [x] 7.6 下单成功后刷新门店看板数据，确认新订单出现在列表中

## 8. 前端：取餐码查询与门店停售

- [x] 8.1 顾客端实现取餐码查询，展示明细、金额与当前状态；未命中时给出明确提示
- [x] 8.2 顾客端实现取消按钮，提交取餐码与手机号后四位；仅在状态为「待制作」时可用，失败时展示服务端返回的错误信息
- [x] 8.3 门店看板侧实现商品停售 / 恢复开关，作用于当前门店并可即时看到状态变化
- [x] 8.4 关键交互元素补齐 `data-testid`，并按需补充或更新 `App.spec.ts`

## 9. 验证与交付

- [x] 9.1 前端 `npm run check && npm run lint && npm test && npm run build` 全部通过
- [x] 9.2 全栈重建：`docker compose down -v && docker compose up -d --build --wait`，三个容器 healthy
- [x] 9.3 端到端人工验收：按门店浏览商品 → 下单拿到取餐码与金额 → 门店看板看到新单 → 推进到制作中 → 顾客侧按取餐码看到最新状态 → 新单取消成功 → 制作中的单取消被拒
- [x] 9.4 回归验收：门店看板 12 条历史订单可见、筛选/详情/推进正常；中文无乱码；超时订单为 4；DB 时间与 API 时间一致
- [x] 9.5 用浏览器实际访问页面截图确认顾客端与门店端均正常渲染
- [x] 9.6 更新 `AGENTS.md`：补充时区、字符集、nginx IPv6 三条环境约定，以及新增的接口与常用验证命令
- [x] 9.7 提交并推送到远端
