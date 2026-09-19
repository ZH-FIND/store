# CoffeeFlow V1 测试报告

| 项 | 内容 |
|---|---|
| 报告对象 | 变更 `add-online-ordering`（顾客线上下单 + 门店商品停售） |
| 需求依据 | [docs/02-requirements/01-prd.md](../02-requirements/01-prd.md)、[原始需求](../00-origin/) |
| 变更依据 | [proposal](../../openspec/changes/add-online-ordering/proposal.md)、[design](../../openspec/changes/add-online-ordering/design.md)、[specs](../../openspec/changes/add-online-ordering/specs/) |
| 被测版本 | `main` @ `03589c6`（实现提交 `157dc03`） |
| 执行时间 | 2026-09-19 |
| 执行人 | 团队（Trae + tdspeckit SDD 流程） |

## 1. 结论

**通过。** 四个验证层级全部通过，通过率 100%，未发现阻塞性缺陷。

| 层级 | 用例数 | 通过 | 失败 |
|---|---|---|---|
| 后端自动化（JUnit + H2） | 28 | 28 | 0 |
| 前端自动化（Vitest + vue-tsc + ESLint） | 16 | 16 | 0 |
| 手动 API / 数据库校验（MySQL 运行期） | 17 | 17 | 0 |
| 浏览器端到端验收 | 19 | 19 | 0 |

遗留项：1 项低优先级改进建议（错误提示语言），2 项已声明的非目标未覆盖，详见 §8、§9。

## 2. 测试环境

| 项 | 版本 / 说明 |
|---|---|
| 操作系统 | Windows 11 专业版 10.0.26200 |
| 容器运行时 | Docker 29.8.0 + Compose v5.5.1（WSL2 后端） |
| 数据库 | MySQL 8.0（容器内，时区 `+08:00`，字符集 `utf8mb4`） |
| 后端 | Java 17、Spring Boot 3.3.13、Spring JDBC（`JdbcTemplate`） |
| 前端 | Vue 3 + TypeScript + Vite 5.4.21 |
| 后端测试库 | H2 内存库（`MODE=MYSQL`，不依赖 MySQL） |
| 镜像源 | Docker 走 `docker.m.daocloud.io` / `docker.1ms.run`（Docker Hub 直连不可用） |

**服务拓扑**：`mysql` + `backend`(8080) + `frontend`(nginx → 4173)，三容器均 `healthy`。

**环境类回归信号**（部署阶段踩坑后固化，见 [AGENTS.md](../../AGENTS.md) 的 Environment Conventions）：
- 前端 nginx 需同时监听 IPv4/IPv6，否则健康检查失败、`docker compose up --wait` 永不返回
- `sql/` 脚本需 `SET NAMES utf8mb4`，否则中文按 CP1252 解读后入库（数据库里就是坏数据）
- mysql 与 backend 需统一 `TZ=Asia/Shanghai`，否则 API 时间比库中早 8 小时、超时订单由 4 变 9

## 3. 复现命令

```bash
# 后端（H2 内存库，不依赖 MySQL）
docker run --rm -v "<repo>/coffeeflow-v0/backend:/workspace" -v "coffeeflow_m2:/root/.m2" \
  -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -B verify

# 前端
cd coffeeflow-v0/frontend && npm.cmd ci
npm.cmd run check && npm.cmd run lint && npm.cmd test && npm.cmd run build

# 全栈（清库重建，自动执行 sql/00 → 01 → 02）
cd coffeeflow-v0 && docker compose down -v && docker compose up -d --build --wait
```

## 4. 后端自动化测试明细（28）

```
[INFO] Tests run: 7,  Failures: 0, Errors: 0, Skipped: 0 -- CoffeeFlowApiTests
[INFO] Tests run: 3,  Failures: 0, Errors: 0, Skipped: 0 -- JdbcBaselineTests
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0 -- OnlineOrderingApiTests
[INFO] Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS  Total time: 27.654 s
```

### 4.1 CoffeeFlowApiTests（7，V0 基线，测试代码未修改）
`healthCheckReportsUp`、`listsAllV0Orders`、`filtersOrdersByStatus`、`returnsOrderDetail`、`returns404ForMissingOrder`、`updatesOrderStatus`、`rejectsEmptyStatusWith400`

### 4.2 JdbcBaselineTests（3，V0 基线，测试代码未修改）
`v0ReadsAndUpdatesTheOrdersFulfillmentTableThroughJdbcTemplate`、`seedDataKeepsDashboardSummaryStable`、`historicalOrdersCanContainDifferentProducts`

> 这两组是**回归红线**：拆表后未做任何放宽，其中 `JdbcBaselineTests` 直接调用 `Order#getProductCode()` / `getItems()` / `count()`，是保留派生 getter 的原因。

### 4.3 OnlineOrderingApiTests（18，本变更新增）

| # | 用例 | 覆盖规格 |
|---|---|---|
| 1 | `listsStores` | 门店列表 |
| 2 | `listsProductsOfExistingStore` | 按门店查商品 |
| 3 | `returns404ForProductsOfMissingStore` | 门店不存在 |
| 4 | `stopSellingOnlyAffectsTheGivenStore` | 停售门店隔离 |
| 5 | `resumesStoppedProduct` | 恢复可售 |
| 6 | `returns404WhenStoppingProductOfMissingStore` | 停售时门店不存在 |
| 7 | `createsOrderAndExposesItOnTheDashboard` | 下单 + 看板可见 |
| 8 | `rejectsOrderWithUnavailableProductAndCreatesNothing` | 停售强拦且不落单 |
| 9 | `rejectsOrderWithInvalidQuantityAndUnknownProduct` | 数量非法 / 未知商品 |
| 10 | `rejectsOrderWithMissingRequiredFields` | 必填校验 |
| 11 | `queriesOrderByPickupCodeOfTodayOnly` | 取餐码仅当日可查 |
| 12 | `cancelsNewOrderAndKeepsItOnTheDashboard` | 待制作可取消且不删单 |
| 13 | `rejectsCancelWhenOrderIsInProgress` | 制作中不可取消 |
| 14 | `rejectsCancelWithMismatchedPhoneOrPickupCode` | 凭据校验 |
| 15 | `rejectsRepeatedCancel` | 重复取消 |
| 16 | `reusesPickupCodeOnDifferentDatesButResetsTodayFrom1001` | 跨天重码 + 当日重置 |
| 17 | `keepsOrderAmountAfterProductPriceChanges` | 金额快照不受改价影响 |
| 18 | `keepsUnitPriceAndTotalIndependentOfSize` | **规格不加价**（design D6） |

## 5. 前端自动化测试明细（16）

```
✓ src/App.spec.ts (16 tests) 809ms
  Test Files  1 passed (1)
       Tests  16 passed (16)
```
`npm run check`（vue-tsc）与 `npm run lint`（ESLint）无输出即通过；`npm run build` 成功产出 `dist/`（`index-vOTch_UW.js 81.57 kB`，gzip 31.54 kB）。

### 5.1 CoffeeFlow V0（6，原有断言未放宽）
`展示汇总和订单列表`、`支持筛选和重置`、`打开详情并展示混合商品明细`、`推进订单状态`、`列表加载失败时展示后端错误并可重试`、`状态更新失败时保留订单看板并显示错误`

### 5.2 CoffeeFlow V1 顾客点单（10，本变更新增）
`默认停留在门店看板，可切换到顾客点单`、`选定门店后加载商品，停售商品可见但禁用`、`购物车支持加减数量并计算合计金额`、`提交订单后展示取餐码与金额并刷新门店看板`、`缺少必填信息时不下单`、`按取餐码查询订单并在待制作时取消`、`取餐码未命中时展示服务端提示`、`制作中的订单不提供取消入口`、`取消失败时展示服务端错误信息`、`门店看板可停售与恢复当前门店的商品`

## 6. 手动 API / 数据库校验（MySQL 运行期，17 项）

后端自动化跑在 H2 上，因此以下用例在**真实 MySQL** 上逐一复测。

| # | 用例 | 实际结果 | 结论 |
|---|---|---|---|
| 1 | `GET /api/health` | `{"status":"UP","database":"UP"}` | 通过 |
| 2 | `GET /api/v1/orders` | `total = 12` | 通过 |
| 3 | `GET /api/v1/orders?status=READY` | `total = 3` | 通过 |
| 4 | `GET /api/v1/orders/CF-1003` | `productCode=CF-BEV-003`、`drinkName=抹茶拿铁`、`size=中杯`、`quantity=3`、`items=[抹茶拿铁,美式,冷萃]` | 通过 |
| 5 | `GET /api/v1/orders/CF-1012` | `productCode=CF-BEV-014` | 通过 |
| 6 | `GET /api/v1/stores` | 5 个门店，首项 `S001/国贸店` | 通过 |
| 7 | `GET /api/v1/stores/S001/products` | 14 个商品，全部 `available=true` | 通过 |
| 8 | `GET /api/v1/stores/S999/products` | HTTP 404 | 通过 |
| 9 | `POST /api/v1/orders`（燕麦拿铁×2 大杯 + 冷萃×1 小杯） | `{CF-1013, pickupCode 1001, totalAmount 92.00, NEW}`，2×32+1×28=92 | 通过 |
| 10 | `GET /api/v1/orders/pickup/1001` | 明细含 `燕麦拿铁(大杯)x2@32.00`、`冷萃(小杯)x1@28.00`，金额 92 | 通过 |
| 11 | 停售 S001 的 `CF-BEV-002` | 返回体 `available=false`；S001 列表为 `false`、**S002 仍为 `true`** | 通过 |
| 12 | 停售后在 S001 下单该商品 | HTTP 400 `Product is not available in store S001: CF-BEV-002`，未落单 | 通过 |
| 13 | 同一商品在 S002 下单 | HTTP 201，`CF-1016 / pickupCode 1004` | 通过 |
| 14 | 取消：手机号不匹配 | HTTP 400 `Phone last 4 digits do not match order: CF-1013` | 通过 |
| 15 | 取消：凭据正确 | HTTP 200，状态 `CANCELLED`，订单仍在看板 | 通过 |
| 16 | 取消：重复提交 / 订单已制作中 | 分别 HTTP 409 `Order already cancelled` / `Order cannot be cancelled in status IN_PROGRESS`，制作中单状态保持不变 | 通过 |
| 17 | 时间与统计一致性 | API `createdAt` = DB `created_at`（`15:45:17`）；`overdue = 4` | 通过 |

附加校验：数据库直查 `orders=12 / order_items=24 / products=14 / stores=5 / store_product=70`（5×14），`total_amount` 与明细汇总不一致行数 = **0**，12 条历史单的 `pickup_code`/`pickup_date`/`phone_last4` 全为空。

## 7. 浏览器端到端验收（19 项）

执行方式：真实浏览器操作 `http://localhost:4173/`，全程截图取证。

**A. 门店看板（V0 回归，5/5）**
1. 页面加载、默认停在门店看板
2. 统计卡 `总订单 12 / 超时订单 4 / 制作中 3 / 待取餐 3`
3. 订单卡 12 张（CF-1001~CF-1012），中文无乱码
4. CF-1003 详情抽屉：明细 `抹茶拿铁、美式、冷萃`、门店 `国贸店`
5. 状态筛选「待取餐」→ 3 张，重置 → 12 张

**B. 顾客点单（7/7）**
6. 切到「顾客点单」Tab
7. 选门店 `S001` 后自动加载商品
8. 商品列表 14 个，图片路径 `/drinks/CF-BEV-001.png`~`014.png`，初始无「不可售」
9. 购物车：燕麦拿铁 +1 至 2 份（￥64.00）+ 冷萃 1 份（￥28.00），合计 ￥92.00
10. 填写姓名/后四位/备注并提交
11. 下单成功展示 `订单号 CF-1013 / 订单金额 ￥92.00 / 取餐码 1001`
12. 回门店看板，新单出现且状态「待制作」

**C. 取餐码查询与取消（4/4）**
13. 用取餐码 1001 查到订单，明细/金额/状态正确
14. 门店看板推进至「制作中」
15. 顾客侧再查，状态变为「制作中」
16. 制作中订单**取消入口已消失**，提示「仅「待制作」状态的订单可以取消」

**D. 门店停售（3/3）**
17. 门店看板「商品停售管理」把国贸店「美式」置为停售
18. 顾客端选国贸店，该商品显示「不可售」且按钮禁用、无法加入购物车
19. 切换到望京店，同一商品仍可售（**门店隔离**）

资源加载：20 条 `PerformanceResourceTiming` 记录全部 HTTP 200，无失败请求；页面无白屏、无错误覆盖层。

## 8. 缺陷与问题清单

| # | 严重度 | 描述 | 状态 |
|---|---|---|---|
| 1 | 高 | 前端 nginx 只监听 IPv4，容器内 `localhost` 解析到 `::1` → 健康检查持续失败、`docker compose up --wait` 永不返回（`run.sh` 挂住），但应用实际可访问 | 已修复（提交 `44f453e`） |
| 2 | 高 | initdb 客户端字符集为 latin1（MySQL 的 latin1 实为 CP1252）→ UTF-8 种子按 CP1252 解读入库，**库中即为坏数据**，全站中文乱码 | 已修复（`sql/01` 加 `SET NAMES utf8mb4`） |
| 3 | 高 | JDBC `serverTimezone=Asia/Shanghai` 与容器实际 UTC 不一致 → API 时间比库中早 8 小时，前端把所有未终结订单误判超时（**超时订单 4 → 9**） | 已修复（统一 `TZ=Asia/Shanghai`） |
| 4 | 低 | 错误提示沿用后端英文（如 `Order not found for pickup code: 1002`），与全站中文不一致 | 待改进（见下） |
| 5 | 信息 | `Order` 保留了派生 getter（`getProductCode`/`getItems` 等，由明细映射，非新增存储字段），用于兼容不允许改动的 `JdbcBaselineTests` | 已知设计取舍 |

关于 #4：这是"前端直接透传服务端 message、后端异常消息沿用 V0 英文风格"的结果，**不属于回归**。若要统一为中文，建议只在前端做一层 `code → 中文文案` 映射，不要改动后端消息（否则会连带影响既有测试断言）。

以上缺陷 1–3 均属 V0 自带问题，只有在"真实 Docker 环境跑一遍"时才会暴露，已把三条写成 [AGENTS.md](../../AGENTS.md) 的 **Environment Conventions（属踩坑沉淀，勿回退）**，并附诊断信号：`DB 时间 = API 时间`、`超时订单 = 4`、`HEX(store_name)` 可见坏数据、前端健康检查必须转 healthy。

## 9. 未覆盖与未验证项

| 项 | 原因 | 风险 |
|---|---|---|
| 并发下单保护 | design 已声明为非目标；取餐码用 `(pickup_code, pickup_date)` 唯一索引兜底，并发冲突时快速失败而非写脏数据 | 低（单机演示场景） |
| 取餐码跨天查询 | design 已声明为非目标 | 低 |
| 门店维护界面 | 门店为只读种子数据，PRD 列为不做 | 无 |
| 浏览器控制台报错采集 | 本次执行的浏览器工具 console/network 采集通道返回空，改用 `PerformanceResourceTiming` 复核（20 条全部 200） | 低，建议后续用 DevTools 手工复核一次 |
| 历史订单的真实杯数 | V0 未记录，迁移时统一按每商品 1 杯（等价 V0 条目数语义），非真实点单数据 | 信息性，不影响业务口径 |

## 10. 回归红线核对

| 红线 | 期望 | 实测 |
|---|---|---|
| V0 三个接口响应字段与结构 | 零变化 | 零变化（`OrderSummaryResponse` / `OrderDetailResponse` 实现文件未修改） |
| 历史订单总数 | 12 | 12 |
| READY / IN_PROGRESS | 3 / 3 | 3 / 3 |
| 超时订单 | 4 | 4 |
| CF-1003 明细 | `[抹茶拿铁, 美式, 冷萃]` | 一致 |
| CF-1012 商品码 | `CF-BEV-014` | 一致 |
| 存量测试 | 10 个全绿 | 10 个全绿（断言未放宽） |
| 中文显示 | 无乱码 | 无乱码 |
| `unit_price` 与规格 | 规格不加价 | 大杯 32 / 小杯 28，均为商品单价 |
| 停售粒度 | 门店隔离 | S001 拒单、S002 正常下单 |

---

**结论：V1 达到可验收状态，建议按变更 `add-online-ordering` 归档。**
