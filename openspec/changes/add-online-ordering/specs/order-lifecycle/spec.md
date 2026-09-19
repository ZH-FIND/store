## ADDED Requirements

### Requirement: 订单状态与流转
订单 MUST 使用既有 5 状态枚举：`NEW`（待制作）、`IN_PROGRESS`（制作中）、`READY`（待取餐）、`COMPLETED`（已完成）、`CANCELLED`（已取消）。顾客下单后订单 MUST 进入 `NEW`。门店侧状态推进 MUST 保持 V0 的单向顺序：`NEW → IN_PROGRESS → READY → COMPLETED`。

#### Scenario: 下单后进入待制作
- **WHEN** 顾客成功提交一笔订单
- **THEN** 该订单状态为 `NEW`

#### Scenario: 门店在看板上单向推进状态
- **WHEN** 门店店员在看板上对一条 `NEW` 订单执行推进
- **THEN** 该订单状态依次变为 `IN_PROGRESS`、`READY`、`COMPLETED`

#### Scenario: 取消不删除订单
- **WHEN** 一条订单被顾客取消
- **THEN** 该订单状态变为 `CANCELLED`，记录仍保留且门店看板仍可查到

#### Scenario: 状态推进接口保持 V0 语义
- **WHEN** 调用 `PATCH /api/v1/orders/{orderId}/status` 并传入一个合法的状态值
- **THEN** 系统直接将该订单状态设置为传入值并返回更新后的订单详情，与 V0 行为一致

### Requirement: 取餐码生成与当日唯一
系统 MUST 在顾客下单时生成取餐码。取餐码 MUST 为 4 位数字，MUST 在当日所有订单中唯一，且 MUST 每日重置。

#### Scenario: 当日首单取餐码
- **WHEN** 当天尚无任何带取餐码的订单，顾客提交订单
- **THEN** 系统为该订单分配取餐码 `1001`

#### Scenario: 当日后续订单取餐码递增
- **WHEN** 当天已存在取餐码，顾客再次提交订单
- **THEN** 系统为该订单分配的取餐码为「当日已用最大取餐码 + 1」

#### Scenario: 跨天重置
- **WHEN** 日期变更后顾客提交订单
- **THEN** 系统为新订单分配的取餐码从 `1001` 重新开始

#### Scenario: 同一取餐码允许在不同日期重复出现
- **WHEN** 两笔订单的取餐码相同但下单日期不同
- **THEN** 系统允许两笔订单同时存在，不视为冲突

#### Scenario: 取餐码查询按当日期限定
- **WHEN** 顾客使用取餐码查询订单
- **THEN** 系统只在当日的订单范围内匹配该取餐码

### Requirement: 金额与单价快照
订单金额与商品单价 MUST 在下单时刻固定并随订单保存。商品价格后续发生的任何变更 MUST NOT 影响已有订单的金额。

#### Scenario: 订单金额按明细汇总
- **WHEN** 顾客提交一笔包含多条商品明细的订单
- **THEN** 每条明细记录其下单时的单价与「单价 × 数量」的小计，订单总额等于各明细小计之和

#### Scenario: 改价不影响已有订单
- **WHEN** 某商品的价格在订单创建后被修改
- **THEN** 该已有订单的总额与明细单价保持不变

#### Scenario: 可售状态变更不影响已有订单金额
- **WHEN** 某商品在其订单创建后被停售
- **THEN** 该已有订单的金额与内容保持不变

#### Scenario: 规格不影响单价与总额
- **WHEN** 顾客对同一商品分别选择小杯与大杯下单
- **THEN** 两条明细的单价相同，`size` 仅作为展示属性被快照，不参与单价与总额的计算

### Requirement: 门店看板按 V0 契约读取订单
拆分为订单头与明细后，V0 的门店看板接口 MUST 保持响应字段与结构零变化，MUST 兼容既有前端与既有测试基线。

#### Scenario: 订单列表响应结构不变
- **WHEN** 调用 `GET /api/v1/orders` 或 `GET /api/v1/orders?status=`
- **THEN** 响应仍为 `{total, orders[]}`，每个订单元素包含 `id`、`storeName`、`customerName`、`productCode`、`drinkName`、`items`、`quantity`、`status`、`createdAt`、`estimatedReadyAt`、`note`

#### Scenario: quantity 表示商品条目数
- **WHEN** 读取一条包含 N 条商品明细的订单
- **THEN** 该订单的 `quantity` 值为 N，即明细条目数，而不是任一商品的杯数

#### Scenario: items 为明细中的饮品名列表
- **WHEN** 读取一条包含多条明细的订单
- **THEN** `items` 为该订单全部明细的饮品名列表，顺序与明细插入顺序一致

#### Scenario: 主商品字段取第一条明细
- **WHEN** 读取一条订单的 `productCode`、`drinkName` 与 `size`
- **THEN** 三个字段均取自该订单的第一条明细

#### Scenario: 历史订单迁移后仍可见
- **WHEN** 12 条历史订单被迁移为订单头与明细后打开门店看板
- **THEN** 12 条订单全部可见，总数、各状态计数与迁移前的测试基线一致
- **AND** 历史订单的 `pickupCode` 与 `phoneLast4` 为空，无法被顾客侧按取餐码查到
