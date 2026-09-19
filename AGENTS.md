# AGENTS.md

> 目的：仓库级长期指令入口。用于沉淀**项目概览、目录地图、常用命令、验证方式、代码风格、生成物策略、安全与脱敏要求**。内容应简洁、可执行、可验证。

## Scope（生效范围与覆盖规则）

- 本文件默认适用于整个仓库（仓库根 = `coffeeflow-v0/`）。
- 子目录可放置更具体的 `AGENTS.md`，其作用域为该目录树，并覆盖父级同类指令。
- 用户/系统级指令（例如任务描述）优先级高于本文件。

## Project Overview（项目概览）

- 一句话描述：CoffeeFlow —— 门店咖啡订单系统。V0 提供门店侧订单看板（查询、筛选、详情、推进状态）；V1 扩展顾客线上下单与门店商品停售管理。
- 主要技术栈：
  - 后端：Java 17、Spring Boot 3.3.13、Spring JDBC（`JdbcTemplate`）、Maven 3.9+
  - 前端：Vue 3 + TypeScript + Vite + Tailwind CSS
  - 构建与运行：Docker Compose
- 关键依赖：
  - MySQL 8（运行期，`localhost:3306/coffeeflow`）
  - H2 内存库（**仅**后端测试，`MODE=MYSQL`，无需外部数据库）
- 版本状态：V0 已交付；V1 需求见 [docs/02-requirements/01-prd.md](docs/02-requirements/01-prd.md)。

## Repo Map（目录地图）

```text
coffeeflow-v0/                 # 仓库根
├── coffeeflow-v0/             # 应用根（docker-compose.yml 所在层）
│   ├── backend/               # Spring Boot 后端
│   │   └── src/main/java/com/coffeeflow/backend/
│   │       ├── controller/    # REST 入口 + 异常映射
│   │       ├── service/       # 业务逻辑
│   │       ├── repository/    # JdbcTemplate 数据访问（SQL 只在这里）
│   │       ├── model/         # 领域对象与枚举
│   │       └── dto/           # 接口请求/响应体
│   ├── frontend/              # Vue 3 前端（Vite）
│   │   └── src/               # App.vue / main.ts / style.css
│   ├── docker-compose.yml     # mysql + backend + frontend
│   └── run.sh                 # 一键启动并等待全部健康
├── sql/                       # MySQL 初始化脚本，被 compose 挂载为 initdb（按文件名顺序执行）
│   ├── 00-create-database.sql # 建库 + 建用户授权
│   ├── 01-orders-and-seed.sql # V0 建表 + 12 条种子订单
│   └── 02-v1-online-ordering.sql # V1 增量迁移：products/stores/store_product/order_items + 改造 orders
├── docs/                      # 人工维护的顶层规范（管方向）
│   ├── 00-origin/             # 原始需求与附件（只读，勿改）
│   └── 02-requirements/       # PRD
├── openspec/                  # SDD 能力规格与变更（管执行）
└── .trae/                     # Trae 技能（openspec-*）
```

## Common Commands（常用命令）

### 安装与初始化

```bash
# 全栈一键启动；首次会自动建库、建表并灌入 12 条种子数据
./run.sh

# 需要重新初始化数据库时
docker compose down -v && ./run.sh
```

### 本地开发

```bash
# 全栈（前端 4173 / 后端 8080 / MySQL 3306）
cd coffeeflow-v0 && docker compose up --build -d --wait

# 仅后端（需本地已有 MySQL）
cd coffeeflow-v0/backend && mvn spring-boot:run

# 仅前端
cd coffeeflow-v0/frontend && npm ci && npm run dev
```

### 测试与质量

```bash
# 后端（H2 内存库，不依赖 MySQL）
cd coffeeflow-v0/backend && mvn verify

# 本机未安装 Maven 时用容器跑（依赖缓存在 coffeeflow_m2 卷，首次约 2 分钟）
docker run --rm -v "c:\Users\hwx376695\Desktop\STORE\coffeeflow-v0\coffeeflow-v0\backend:/workspace" -v "coffeeflow_m2:/root/.m2" -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -B verify

# 前端（注意：本机禁止运行 .ps1，需用 npm.cmd）
cd coffeeflow-v0/frontend && npm.cmd ci && npm.cmd run check && npm.cmd run lint && npm.cmd test && npm.cmd run build
```

## API 一览

V0（契约冻结，不得改动字段名与结构）：

```
GET   /api/health
GET   /api/v1/orders?status=
GET   /api/v1/orders/{orderId}
PATCH /api/v1/orders/{orderId}/status
```

V1（顾客下单与门店商品管理）：

```
GET   /api/v1/stores                                              # 门店列表
GET   /api/v1/stores/{storeId}/products                           # 该门店商品（含门店级 available）
PATCH /api/v1/stores/{storeId}/products/{productCode}/availability # 门店停售 / 恢复，body {available}
POST  /api/v1/orders                                              # 顾客下单 → 201，返回 orderId/pickupCode/totalAmount/status
GET   /api/v1/orders/pickup/{pickupCode}                          # 按取餐码查当日订单
POST  /api/v1/orders/{orderId}/cancel                             # body {pickupCode, phoneLast4}
```

## Verification Gate（验证门禁）

- **不允许**在无验证证据的情况下宣称“已完成/已修复/已通过”。
- 变更最小验证集（MUST）：
  - [ ] 后端：`mvn verify` 通过
  - [ ] 前端：`npm run check`、`npm run lint`、`npm test`、`npm run build` 全部通过
  - [ ] 涉及接口或数据库改动：全栈 `docker compose up --build --wait` 能起来，且相关接口实际调通（附请求/响应证据）
- **V0 回归红线**（任何改动都不得破坏）：
  - `GET /api/v1/orders?status=`、`GET /api/v1/orders/{orderId}`、`PATCH /api/v1/orders/{orderId}/status` 的响应**字段名与结构零变化**
  - `CoffeeFlowApiTests`（7）、`JdbcBaselineTests`（3）、`OnlineOrderingApiTests`（18）必须全绿；前两组锁死 total=12、READY=3、IN_PROGRESS=3、overdue=4、CF-1003 明细、CF-1012 的商品码
  - 门店订单看板的列表、筛选、详情抽屉、推进状态均正常
- **V1 业务红线**（改动不得回退）：
  - `unit_price` / `total_amount` 一律取商品统一价，**规格（小/中/大杯）不加价**
  - 取餐码 4 位数字、**当日唯一、每日从 1001 重置**；查询只按当日匹配非空取餐码
  - 顾客取消仅限「待制作」，且必须同时匹配取餐码与手机号后四位；取消只改状态不删单
  - 停售按**门店 × 商品**隔离，且服务端强制拦截下单（不依赖前端）
  - 商品明细只能落在 `order_items`；**不得在 `orders` 上恢复商品字段**（`quantity` 是明细条目数，不是杯数）

## Code Conventions（代码约定）

- 优先复用现有模式，避免引入不必要的新抽象。
- 避免“顺手重构”无关文件；变更应聚焦且可回滚。
- 后端分层固定为 `controller → service → repository`：
  - Controller 只做 HTTP 编解码与异常映射（`NoSuchElementException` → 404，`IllegalArgumentException` → 400）
  - SQL 只出现在 `repository/`
- 数据访问统一使用 `JdbcTemplate`，不引入 JPA / MyBatis。
- DTO 命名：请求 `*Request`，列表 `*ListResponse`，详情 `*DetailResponse`。
- 前端为单文件组件（`.vue`），交互元素带 `data-testid`，便于测试定位。
- 命名约定：Java 类 `PascalCase`、方法与字段 `camelCase`；前端变量与函数 `camelCase`、组件 `PascalCase`；数据库列与表 `snake_case`。
- 订单的金额、单价等业务快照在写入时固定，不随商品表后续变更而重算。

## Environment Conventions（环境约定，属踩坑沉淀，勿回退）

以下三条是部署阶段实际踩到并已修复的问题，改动相关文件时 MUST 保留，否则 V0 在 Docker 环境下会再次不可用：

- **时区必须全链路一致为 `Asia/Shanghai`**：`docker-compose.yml` 中 mysql 与 backend 均设 `TZ: Asia/Shanghai`，mysql 追加 `command: ["--default-time-zone=+08:00"]`，JDBC URL 保持 `serverTimezone=Asia/Shanghai`，时间字段统一用无时区的 `LocalDateTime`。
  三者不一致时 `resultSet.getTimestamp().toLocalDateTime()` 会隐式换算，API 返回的时间比库中早 8 小时，前端进而把所有未终结订单误判为“超时”（超时订单由 4 变 9）。**验收信号：DB 时间必须等于 API 时间，且超时订单为 4。**
- **字符集必须为 `utf8mb4`**：`sql/` 下每个含中文的初始化脚本首行 MUST 有 `SET NAMES utf8mb4;`。
  官方 mysql 镜像的 initdb 客户端默认字符集是 latin1（MySQL 的 latin1 实为 CP1252），缺少该语句会把 UTF-8 种子文件按 CP1252 解读后入库，导致所有中文显示成 `å›½è´¸åº—` 之类的乱码，且**数据库里存的就是坏数据**（`HEX(store_name)` 可见）。
- **前端 nginx 必须同时监听 IPv4 与 IPv6**：`frontend/nginx.conf` 的 `listen 80;` 之后 MUST 保留 `listen [::]:80;`。
  容器内 `localhost` 解析到 `::1`，只监听 IPv4 会使健康检查持续失败、`docker compose up --wait` 永不返回（`./run.sh` 挂住），而应用本身却能被访问到 —— 现象容易被误判为“已经部署成功”。

## Generated Artifacts（生成物策略）

- 本仓库无代码生成器，无生成物入库。
- 构建产物按需生成、不入库：
  - 后端 `mvn package` → `backend/target/`
  - 前端 `npm run build` → `frontend/dist/`
- 禁止手工编辑上述构建产物。

## Security & Desensitization（安全与脱敏）

- 禁止提交：密钥、Token、Cookie、私钥、连接串（含密码）、生产账号信息。
- `docker-compose.yml` 中的 `root/root`、`coffeeflow/coffeeflow` 为**本地示例口令**，不得复用到任何真实环境。
- 顾客个人信息最小化：手机号只保留后四位（`phone_last4`），不存储或提交完整号码。
- 示例数据一律使用假数据（如“顾客2”“Alice”“000000”）。
- 日志与错误信息避免输出完整 PII。

## Working Style（建议的默认工作流）

1. Explore：定位相关文件与现有模式
2. Plan：列出要改文件/步骤/验证
3. Implement：小步修改，避免无关改动
4. Verify：跑最小验证集（必要时扩大）
5. Summarize：说明改了什么、为什么、怎么验证

## 文档双层与变更流程

- `docs/` 管方向：顶层规划、需求（PRD）、架构、ADR —— 低频、人工维护。
- `openspec/` 管执行：能力规格（`specs/`）与单次变更（`changes/`）—— 高频、人机协同。
- 单次变更走 SDD 闭环，在 Trae 中以技能方式调用：
  `openspec-explore` → `openspec-propose` → `openspec-apply-change` → `openspec-archive-change`
