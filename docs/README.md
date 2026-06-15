# 图书馆智能管理系统 — 文档索引

> 本目录包含项目的全部设计文档、开发指南与实施记录。按阅读顺序排列。

---

## 阅读导航

### 新人入门（按顺序阅读）

| 顺序 | 文档 | 说明 |
|------|------|------|
| 1 | [系统架构设计文档](系统架构设计文档.md) | **开发蓝本**。系统目标、技术架构、模块设计、数据库设计、API 规范、核心算法 |
| 2 | [DEVELOPMENT](DEVELOPMENT.md) | **环境搭建**。依赖安装、Docker 中间件、IDE 配置、启动验证 |
| 3 | [CONTRIBUTING](CONTRIBUTING.md) | **编码规范**。Git 工作流、Commit 规范、Java/Android 编码标准、Code Review |

### 实施推进

| 文档 | 说明 |
|------|------|
| [implementation/后端分阶段实施计划](implementation/后端分阶段实施计划.md) | 11 阶段的完整实施路线图，含 126 项具体任务、依赖关系、验证清单 |
| [implementation/阶段0完成记录](implementation/阶段0完成记录.md) | 阶段 0（基础设施底座）完成详情与架构决策记录 |

### 技术参考

| 文档 | 说明 |
|------|------|
| [api/library-api.yaml](api/library-api.yaml) | OpenAPI 3.0 规范，前后端数据契约（28 个端点，完整 Schema + Example） |
| [db/init.sql](db/init.sql) | Docker MySQL 容器首次启动时的字符集初始化脚本 |

---

## 目录结构

```
docs/
├── README.md                           # ← 本文档（索引）
├── 系统架构设计文档.md                  # 设计蓝本
├── DEVELOPMENT.md                      # 开发环境指南
├── CONTRIBUTING.md                     # 编码规范与协作指南
├── api/
│   └── library-api.yaml               # OpenAPI 契约
├── db/
│   └── init.sql                       # Docker MySQL 初始化
└── implementation/                     # 实施计划与进度记录
    ├── 后端分阶段实施计划.md            # 11 阶段实施路线图
    └── 阶段0完成记录.md                 # 阶段 0 完成详情
```

---

## 文档维护约定

- **设计文档**（架构、API）在对应功能变更时同步更新
- **开发指南**（DEVELOPMENT、CONTRIBUTING）随工具链/流程调整而更新
- **实施记录**每完成一个阶段追加，格式参照 `阶段0完成记录.md`
- **命名**：核心永久文档用中文（便于团队阅读），工程规范文档沿用 Conventional 英文名（CONTRIBUTING/DEVELOPMENT 为社区通用文件名）
