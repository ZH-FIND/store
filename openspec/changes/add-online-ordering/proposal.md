# add-online-ordering

## Why

V0 只有门店侧的单据视图：订单由后台灌入，店员只能查看和推进状态。顾客无法自助点单，门店也无法控制商品是否可卖。

更关键的是，V0 的数据模型把「订单头」与「订单里的商品」压在同一行 —— 一个订单多商品时只记录商品名，**没有每个商品的数量和单价**，因此无法计算订单金额。V0 的 `quantity` 字段在全部 12 条种子数据里恒等于商品条目数，语义上并不是"杯数"。不解决这个模型问题，V1 要求的下单与金额无处落脚。

## What Changes

- 新增商品目录（14 个商品，含价格）与门店目录（5 个门店，只读）
- 新增「门店 × 商品」可售关系，支持门店临时停售 / 恢复商品
- **BREAKING（数据模型）**：`orders` 拆分为订单头 + `order_items` 明细；原 `product_code` / `drink_name` / `size` / `quantity` / `items` 字段移除，由明细聚合得出
- 新增顾客下单能力：选门店 → 选商品与数量 → 提交 → 返回订单金额与取餐码
- 新增取餐码：4 位数字，**当日唯一、每日重置**
- 新增按取餐码查询订单（仅当日有效码；12 条历史订单取餐码置空，永不可被顾客查到）
- 新增顾客取消：仅「待制作」状态可取消，需校验取餐码 + 手机号后四位
- 顾客端界面以 Tab 形式并入现有单页应用，不引入前端路由
- **不改动** V0 的既有接口：`GET /api/v1/orders`、`GET /api/v1/orders/{orderId}`、`PATCH /api/v1/orders/{orderId}/status` 的响应字段与结构保持零变化

## Capabilities

### New Capabilities

- `customer-ordering`: 顾客侧完整闭环 —— 按门店浏览商品（名称/图片/价格/可售状态）、选择商品与数量下单、获得订单金额与取餐码、凭取餐码查询订单状态、在门店开始制作前取消订单
- `store-product-availability`: 门店侧商品可售管理 —— 门店店员针对本门店停售或恢复某商品，停售只影响本门店且服务端强制拦截该门店下单
- `order-lifecycle`: 订单的公共规则 —— 状态机与流转约束、取餐码生成与查询规则、金额与单价快照、以及门店看板在拆表后仍按 V0 契约读取订单

### Modified Capabilities

无。`openspec/specs/` 当前为空，V0 行为尚无既有能力规格。

## Impact

**数据库**：新增 `products`、`stores`、`store_product`、`order_items` 四张表；改造 `orders`（移除 5 个字段，新增 `phone_last4`、`pickup_code`、`pickup_date`、`total_amount`）。新增 DDL 与种子迁移脚本落在 `sql/`，与现有 `00` / `01` 脚本同一套初始化流程。12 条历史订单需迁移为订单头 + 明细。

**后端**：新增商品 / 门店 / 下单 / 取餐码查询 / 取消 / 停售 六组接口；`OrderRepository` 由单表读写改为订单头与明细的聚合读写（SQL 仍只出现在 `repository/`）。

**前端**：`App.vue` 增加 Tab 切换与顾客端视图（门店选择、商品列表、购物车、下单结果、取餐码查询）；门店看板侧新增停售开关。

**回归红线**：V0 的 3 个接口契约不变，`CoffeeFlowApiTests` 与 `JdbcBaselineTests` 必须保持全绿（锁死 total=12、READY=3、IN_PROGRESS=3、overdue=4 及两条明细断言）。

**外部依赖**：无新增。技术栈沿用 Java 17 / Spring Boot 3.3 / JdbcTemplate / MySQL 8 / Vue 3。

**明确不做**：支付与退款、账号与登录体系、门店维护界面、库存扣减、并发下单保护、取餐码跨天查询。
