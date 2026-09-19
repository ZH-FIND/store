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
├── sql/                       # MySQL 初始化脚本，被 compose 挂载为 initdb
│   ├── 00-create-database.sql # 建库 + 建用户授权
│   └── 01-orders-and-seed.sql # 建表 + 12 条种子订单
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

# 前端
cd coffeeflow-v0/frontend && npm ci && npm run check && npm run lint && npm test && npm run build
```

## Verification Gate（验证门禁）

- **不允许**在无验证证据的情况下宣称“已完成/已修复/已通过”。
- 变更最小验证集（MUST）：
  - [ ] 后端：`mvn verify` 通过
  - [ ] 前端：`npm run check`、`npm run lint`、`npm test`、`npm run build` 全部通过
  - [ ] 涉及接口或数据库改动：全栈 `docker compose up --build --wait` 能起来，且相关接口实际调通（附请求/响应证据）
- **V0 回归红线**（任何改动都不得破坏）：
  - `GET /api/v1/orders?status=`、`GET /api/v1/orders/{orderId}`、`PATCH /api/v1/orders/{orderId}/status` 的响应**字段名与结构零变化**
  - `CoffeeFlowApiTests`、`JdbcBaselineTests` 必须全绿（锁死 total=12、READY=3、IN_PROGRESS=3、overdue=4、CF-1003 明细、CF-1012 的商品码）
  - 门店订单看板的列表、筛选、详情抽屉、推进状态均正常

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
