# 技术方案：add-online-ordering

## Context

CoffeeFlow V0 是一个单表应用：`orders` 一张表同时承载订单头与订单内的商品信息，后端只有查询与改状态两类接口，前端是一个无路由的单页看板。V0 已在本机 Docker 环境部署验证通过，并有 `CoffeeFlowApiTests` / `JdbcBaselineTests` 两组测试锁死基线（total=12、READY=3、IN_PROGRESS=3、overdue=4，以及两条明细断言）。

V1 需要引入顾客下单，而 V0 的模型无法支撑：`quantity` 在 12 条种子数据里恒等于 `items` 的条目数，是"商品条目数"而非"杯数"；订单行里没有单价，算不出金额。

部署阶段已经踩到并修复了三个环境问题，方案必须避免回归：nginx 缺 IPv6 监听导致健康检查失败、initdb 客户端字符集为 latin1 导致中文乱码入库、容器时区不一致导致时间偏移 8 小时。

约束：2 小时练习项目，优先"能跑通 + 能验收"，明确接受部分简化。

本方案由 [docs/02-requirements/01-prd.md](../../../docs/02-requirements/01-prd.md) 推导而来。方案自身的决策编号（D1–D11）与 PRD 的决策编号（D1–D6）含义不同，对应关系见 Decisions 末尾的追溯表。

## Goals / Non-Goals

**Goals:**

- 用规范化模型替换 V0 的单表模型，使「一单多商品 + 每商品数量 + 单价」成为一等公民
- V0 的 3 个接口在字段与结构上零变化，既有测试基线不修改即通过
- 落地顾客下单闭环：浏览 → 下单 → 取餐码 → 查询 → 取消
- 落地门店级商品停售，且服务端强制拦截
- 保持一套初始化脚本即可从零起环境（`docker compose down -v && ./run.sh`）

**Non-Goals:**

- 支付、退款、优惠券、会员
- 账号与登录；不做鉴权
- 门店维护界面（门店为只读种子数据）
- 库存扣减、产能与排队
- 并发下单保护：取餐码生成的并发竞争本版不处理
- 取餐码跨天查询

## Decisions

### D1 拆表：`orders` 只留订单头，明细进 `order_items`

**选择**：移除 `orders` 的 `product_code` / `drink_name` / `size` / `quantity` / `items`，新增 `order_items` 承载「订单 × 商品 × 数量 × 单价快照」。

**备选**：① 保留旧列并双写；② V0 表完全不动、另建线上订单表。

**理由**：双写会引入两个事实来源，顾客下单路径与看板读取路径容易不一致；另建表会让门店看板必须同时读两张表，长期更贵。规范化后单一事实来源，代价是 V0 的读取要改成聚合，而聚合逻辑是纯读取、可被既有测试直接覆盖，回归风险可控。

### D2 V0 兼容靠「聚合读取」，不改接口契约

**选择**：`OrderRepository` 改为按订单头 + 明细两次查询后在 Java 组装（订单头 `ORDER BY id`，明细按 `order_id IN (...)` 一次取回，用 `Map<String, List<Item>>` 分组）。

**备选**：单条 JOIN + `GROUP_CONCAT` 一把梭。

**理由**：两次查询 + Java 分组的结果集映射简单直白，避免 `GROUP_CONCAT` 的长度限制与分隔符转义问题；数据量只有十几条，性能无差异。字段映射规则固定为：`quantity` = 明细条数、`items` = 明细饮品名列表、`productCode`/`drinkName`/`size` = 第一条明细。`order_items` 需要 `line_no` 以稳定还原顺序。

### D3 取餐码：4 位数字，当日唯一，每日重置

**选择**：下单事务内取「当日已用最大取餐码 + 1」，当日首单为 `1001`，跨天自动回到 `1001`。为 `orders` 增加 `pickup_date`，并建 `(pickup_code, pickup_date)` 唯一索引兜底。

**备选**：全局唯一递增（不复用）、独立序列表加行锁。

**理由**：业务方明确要求每日重置。用 `MAX + 1` 而非序列表，是为 2 小时目标做的取舍 —— 不加锁，单机演示不会并发；唯一索引保证即使并发也不会写出重复数据（会报错而非静默脏数据）。序列表方案留作后续加固。

### D4 停售：门店 × 商品关系表

**选择**：新建 `store_product(store_id, product_code, available)`，主键 `(store_id, product_code)`，种子为 5 门店 × 14 商品全部可售。商品列表查询按 `store_id` 左连接该表返回 `available`。

**备选**：`products.available` 全局开关。

**理由**：业务方明确要求按门店。用户故事五是"门店店员停售"，全局开关会让 A 店停售影响 B 店。代价是多一张关系表与一次 join，但语义正确。

### D5 顾客身份：手机号后四位 + 取餐码

**选择**：下单时记录 `customer_name` 与 `phone_last4`；取消订单需同时匹配取餐码与 `phone_last4`。

**备选**：仅凭取餐码；完整手机号。

**理由**：无账号体系下，"取餐码 + 后四位"是成本最低的双因子 —— 取餐码是公开信息（会显示在取餐屏上），单靠它任何人都能取消别人的订单。只存后四位同时符合个人信息最小化。

### D6 价格模型：商品统一价，规格（小/中/大杯）不加价

**选择**：`products` 只存一个 `price`；`size` 仅作为订单明细的展示属性被快照，不参与任何金额计算。

**备选**：商品基础价 + 规格加价；按「商品 × 规格」拆成 SKU 各自定价。

**理由**：PRD 已拍板规格不加价。SKU 化会让 14 个商品的种子膨胀成 42 条，停售粒度也要跟着下沉，2 小时目标下明显不划算。**这是硬约束：任何实现都不得让 `size` 影响 `unit_price` 或 `total_amount`。**

### D7 订单与门店的关联：订单冗余保存 `store_name`

**选择**：`orders` 不新增 `store_id`，继续保留 `store_name`；顾客下单提交 `storeId`，服务端校验门店存在后把对应的 `store_name` 落入订单。`orders` 与 `stores` 之间不建外键。

**备选**：`orders` 改存 `store_id` 并 join 取名字；`store_id` 与 `store_name` 都存。

**理由**：V0 的接口契约与看板门店筛选都基于 `store_name`（前端门店下拉由订单里的门店名去重得出），保留它可让 V0 侧完全不动。订单本身是历史凭证，门店改名不应追溯改变历史订单的展示；不建外键也避免门店数据被误删时波及订单。代价是 `store_name` 冗余、更名后新旧订单显示不同名字 —— 在门店为只读种子的前提下可接受。

### D8 前端：单页 Tab 切换，不引入 vue-router

**选择**：在现有 `App.vue` 中以 Tab 切换「门店看板」与「顾客点单」两个视图。

**备选**：启用已安装但未使用的 `vue-router` 拆成两个视图。

**理由**：2 小时目标下 Tab 的改动量最小，且不改变构建与部署产物结构。代价是 `App.vue` 会变长，后续如继续扩展应重构为路由。

### D9 时区与字符集：全链路固定为 Asia/Shanghai + utf8mb4

**选择**：mysql 与 backend 容器均设 `TZ: Asia/Shanghai`，mysql 追加 `--default-time-zone=+08:00`；`sql/` 下脚本首行 `SET NAMES utf8mb4`；JDBC URL 保持 `serverTimezone=Asia/Shanghai`。时间字段一律用无时区的 `LocalDateTime`。

**理由**：这是本次部署实际踩过的坑。三者必须一致，否则 `resultSet.getTimestamp().toLocalDateTime()` 会做一次隐式时区换算，导致 API 返回的时间比库中早 8 小时，进而使前端把所有未终结订单误判为"超时"（超时订单从 4 变成 9）。这些约定需写入 `AGENTS.md` 防止后续改动重新引入。

### D10 历史订单迁移：每个商品 1 杯，取餐码与手机号置空

**选择**：迁移时按 `orders.items` 用 `|` 拆分生成明细，每条明细数量为 1（等价于 V0 的"条目数"语义），`size` 取原订单的 `size`，`product_code` 按 `drink_name` 反查 `products`，`unit_price` 取商品现价。历史订单的 `pickup_code` / `pickup_date` / `phone_last4` 置为 `NULL`，因此按取餐码查询（限定当日且要求取餐码非空）天然查不到历史单。

**理由**：V0 没有记录每商品杯数，任何反推都是编造；按 1 杯迁移可让 `quantity` 聚合结果与迁移前完全一致，测试基线不需要修改。取餐码留空是业务方确认的"需要清理"。

### D11 数据库变更以增量脚本交付

**选择**：不修改 V0 的 `sql/00-create-database.sql` 与 `sql/01-orders-and-seed.sql` 的业务内容（仅保留已提交的 `SET NAMES utf8mb4` 修复），新增 `sql/02-v1-online-ordering.sql` 完成：建 `products` / `stores` / `store_product` / `order_items` 并灌种子 → 由旧 `orders` 派生 `order_items` 明细 → 为 `orders` 增加新列并回填 `total_amount` → 删除 `orders` 的 5 个旧列 → 建取餐码唯一索引。

**理由**：`sql/` 目录被 compose 挂载为 initdb，按文件名顺序执行，因此增量脚本能让全新环境直接得到 V1 结构；同时保留 V0 基线脚本原貌，使"V0 → V1"的模型演进退化可读。迁移顺序上先派生明细再删旧列，保证数据不丢。

### 与 PRD 决策的追溯

> 注意：本节左列是**本方案**的编号，右列是 **PRD** 的编号，两者含义不同。

| 本方案 | 决策要点 | 对应 PRD | 对应 spec |
|---|---|---|---|
| D1 | 规范化拆表（`orders` + `order_items`） | PRD D1 | `order-lifecycle` |
| D2 | V0 兼容靠聚合读取，接口契约不变 | PRD §4.3 | `order-lifecycle` |
| D3 | 取餐码 4 位数字、当日唯一、每日重置 | PRD D2 | `order-lifecycle`、`customer-ordering` |
| D4 | 停售粒度 = 门店 × 商品 | PRD D6 | `store-product-availability` |
| D5 | 顾客身份 = 手机号后四位 | PRD D3 | `customer-ordering` |
| D6 | 价格模型 = 统一价、规格不加价 | **PRD D4** | `order-lifecycle`（规格不影响金额） |
| D7 | 订单冗余保存 `store_name`，不建外键 | PRD §4.2（图与字段表之间的歧义，此处裁定） | `order-lifecycle` |
| D8 | 前端单页 Tab 切换 | PRD D5 | — |
| D9 | 时区 / 字符集全链路固定 | PRD §8 部署约束 | — |
| D10 | 历史订单迁移口径（每商品 1 杯、取餐码置空） | PRD §4.4 | `order-lifecycle` |
| D11 | 数据库变更以增量脚本交付 | PRD §8 数据库变更约束 | — |

> PRD 的 §5 业务规则 R1–R9 未在此单列决策，它们直接落到三份 spec 的场景中。

## Risks / Trade-offs

- **拆表破坏 V0 看板** → 3 个接口的响应契约由 `CoffeeFlowApiTests` 的字段断言直接锁死；聚合规则（明细条数、明细名列表、第一条明细作为主商品）在 spec 中显式定义，实现后必须跑通既有测试，并用浏览器复验看板列表、筛选、详情抽屉、推进状态。
- **取餐码并发重复** → 已声明为非目标；用 `(pickup_code, pickup_date)` 唯一索引兜底，冲突时快速失败而非写入脏数据。若后续需要，再引入序列表加行锁。
- **时区/字符集回归** → 三个环境约定固化在 `docker-compose.yml`、`sql/` 与 `AGENTS.md`；验收时显式比对「DB 时间 = API 时间」与「中文不乱码」，并把"超时订单 = 4"作为回归信号。
- **单页 `App.vue` 过长** → 2 小时目标下接受；tasks 中不安排前端重构，避免范围蔓延。
- **历史订单每笔只有 1 杯** → 与 V0 语义等价但并非真实点单数据；已在 spec 与设计中标明，不作为业务口径。
- **取消接口对已取消订单重复调用** → 返回明确错误而非幂等成功；语义更清晰，前端只需提示。

## Migration Plan

1. 新增 `sql/02-v1-online-ordering.sql`（建表 → 灌种子 → 派生明细 → 改造 `orders` → 建索引）
2. 后端新增商品 / 门店 / 下单 / 取餐码查询 / 取消 / 停售 六组接口；`OrderRepository` 改为聚合读取
3. 前端 `App.vue` 增加 Tab 与顾客端视图、门店停售开关
4. 验证：`mvn verify`（既有基线必须不动即通过）→ 前端 `check` / `lint` / `test` / `build` → `docker compose down -v && docker compose up -d --build --wait` 全栈联调
5. 回滚：本版未提交前直接丢弃工作区改动；已提交后回滚该变更的提交，并执行 `docker compose down -v` 以回到 V0 结构（历史库数据可重新由 `01` 种子重建）

## Open Questions

- 14 个商品的具体价目（业务方已确认"自由定价"，实现时自拟一组≥1 位小数的价格即可）
- 下单后 `estimated_ready_at` 如何估算（V0 该字段为 NOT NULL）—— 本版按下单时间 + 15 分钟生成占位值，不参与任何业务规则
- 顾客端是否需要保留"最近一次下单"的本地记忆（刷新后仍能回看取餐码）—— 本版不做，靠顾客自行记录取餐码
