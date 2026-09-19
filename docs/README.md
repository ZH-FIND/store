# 项目文档

本目录存放项目文档，由 `openspec init` 生成。不受 `openspec update` 管理——这些文件由你自主维护。

## 名称说明

你会在项目中看到多个名称，它们的关系如下：

| 名称 | 说明 |
|------|------|
| `tdspeckit` | npm 包名，通过 `npm install -g tdspeckit@latest` 安装 |
| `tdspec` | CLI 命令别名，用法与 openspec 完全一致 |
| `openspec` | CLI 命令，在终端中使用：`openspec init`、`openspec update` |
| `/opsx:` | AI 编码工具中的斜杠命令前缀（Cursor、OpenCode、Claude Code 等） |
| `openspec/` | `openspec init` 创建的目录，沿用上游命名约定以保证兼容性 |
| `OPENSPEC_*` | 环境变量前缀（如 `OPENSPEC_TELEMETRY`） |

> `openspec/` 目录名沿用了上游（Fission-AI/OpenSpec）约定，保留此名称以确保与内部工具链和后续上游合并的兼容性。你安装的包是 `tdspeckit`，使用的命令是 `openspec`（`tdspec` 为等价别名）。

## 目录结构

| 目录 | 用途 |
|------|------|
| [01-planning/](01-planning/README.md) | 项目章程、路线图和规划文档 |
| [02-requirements/](02-requirements/README.md) | 产品需求、PRD 和功能规格 |
| [03-architecture/](03-architecture/README.md) | 系统架构、技术设计和决策记录 |
| [04-adr/](04-adr/README.md) | 架构决策记录（ADR） |

## 新人推荐路径

如果你是第一次接触 SDD，建议按以下顺序阅读和编写文档：

1. 从 [01-planning/](01-planning/README.md) 开始——创建项目章程，明确项目要解决的问题
2. 进入 [02-requirements/](02-requirements/README.md) ——参考 [openspec/specs/](../openspec/specs/) 中已有的能力规格来编写需求
3. 查看 [03-architecture/](03-architecture/README.md) ——参考 [openspec/changes/](../openspec/changes/) 中活跃的设计决策
4. 使用 [04-adr/](04-adr/README.md) 记录重要的架构决策

## 可选目录

根据需要自行创建：

- `api/` — API 文档和接口规范
- `deployment/` — 部署指南和基础设施文档
- `operations/` — 运维手册、监控和故障处理
- `quality/` — 测试策略、QA 流程和质量标准
- `references/` — 外部参考资料、调研和学习资源
- `reports/` — 状态报告、会议记录和回顾总结
