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
| [implementation/阶段1完成记录](implementation/阶段1完成记录.md) | 阶段 1（安全与认证）完成详情与架构决策记录 |
| [implementation/阶段2完成记录](implementation/阶段2完成记录.md) | 阶段 2（核心业务—数据层）完成详情与架构决策记录 |
| [implementation/阶段3完成记录](implementation/阶段3完成记录.md) | 阶段 3（核心业务—图书检索）完成详情与架构决策记录 |
| [implementation/阶段4完成记录](implementation/阶段4完成记录.md) | 阶段 4（核心业务—借阅与预约）完成详情与架构决策记录 |
| [implementation/阶段5完成记录](implementation/阶段5完成记录.md) | 阶段 5（AI 基础设施）完成详情与架构决策记录 |
| [implementation/阶段6完成记录](implementation/阶段6完成记录.md) | 阶段 6（图书推荐引擎）完成详情与架构决策记录 |
| [implementation/阶段6审计修复记录](implementation/阶段6审计修复记录.md) | 阶段 6 后跨阶段综合质量审计修复（P0-P2 + 文档/契约同步） |
| [implementation/阶段7完成记录](implementation/阶段7完成记录.md) | 阶段 7（学科知识图谱）完成详情——Neo4j 图谱构建/查询/溯源/主题网络 |
| [implementation/阶段8完成记录](implementation/阶段8完成记录.md) | 阶段 8（智能采编）完成详情——ARIMA 采购预测/查重查缺/智能谈判 |
| [implementation/阶段8后审计修复记录](implementation/阶段8后审计修复记录.md) | 阶段 0-8 四维度质量审计（实现质量/阶段配合/文档维护/架构落地），三轮修复全记录 |
| [implementation/阶段9完成记录](implementation/阶段9完成记录.md) | 阶段 9（系统管理与监控）完成详情——用户管理/流通统计/操作日志/定时任务/Prometheus |
| [implementation/阶段9后审计修复记录](implementation/阶段9后审计修复记录.md) | 阶段 0-9 四维度质量审计（3P0+16P1+29P2+33P3）+ 三批修复落地（62747e0 / 剩余修复 / 回溯修正），全量 360 项测试全绿 |
| [implementation/阶段9后第二轮回溯审计修复记录](implementation/阶段9后第二轮回溯审计修复记录.md) | 阶段 0-9 第二轮回溯复审（6P1+13P2+20P3=39 项），含 JWT 默认密钥回归修复，全量 362 项测试全绿 |
| [implementation/前端人员A完成记录](implementation/前端人员A完成记录.md) | Android 前端人员 A — 基础架构 + 认证 + 图书检索 + 个人中心 完成详情 |
| [implementation/前端人员B完成记录](implementation/前端人员B完成记录.md) | Android 前端人员 B — 借阅管理/预约管理/知识图谱/系统管理/条码扫描 完成详情 |

### 技术参考

| 文档 | 说明 |
|------|------|
| [api/library-api.yaml](api/library-api.yaml) | OpenAPI 3.0 规范，前后端数据契约（46 个端点：全部已实现 ✅，完整 Schema + Example） |
| [db/init.sql](db/init.sql) | Docker MySQL 容器首次启动时的字符集初始化脚本 |

---

## 目录结构

```
docs/
├── README.md                           # ← 本文档（索引）
├── 系统架构设计文档.md                  # 设计蓝本（v1.14）
├── DEVELOPMENT.md                      # 开发环境指南
├── CONTRIBUTING.md                     # 编码规范与协作指南
├── api/
│   └── library-api.yaml               # OpenAPI 契约（46 端点，全部已实现）
├── db/
│   └── init.sql                       # Docker MySQL 初始化
└── implementation/                     # 实施计划与进度记录
    ├── 后端分阶段实施计划.md            # 11 阶段实施路线图
    ├── 阶段0完成记录.md                 # 阶段 0 完成详情
    ├── 阶段1完成记录.md                 # 阶段 1 完成详情
    ├── 阶段2完成记录.md                 # 阶段 2 完成详情
    ├── 阶段3完成记录.md                 # 阶段 3 完成详情
    ├── 阶段4完成记录.md                 # 阶段 4 完成详情
    ├── 阶段5完成记录.md                 # 阶段 5 完成详情
    ├── 阶段6完成记录.md                 # 阶段 6 完成详情
    ├── 阶段6审计修复记录.md             # 阶段 6 后跨阶段综合审计修复
    ├── 阶段7完成记录.md                 # 阶段 7 完成详情
    ├── 阶段8完成记录.md                 # 阶段 8 完成详情
    ├── 阶段8后审计修复记录.md           # 阶段 0-8 四维度质量审计（三轮修复）
    ├── 阶段9完成记录.md                 # 阶段 9（系统管理与监控）完成详情
    ├── 阶段9后审计修复记录.md           # 阶段 0-9 四维度审计 + 三批修复落地（合并版）
    ├── 阶段9后第二轮回溯审计修复记录.md  # 第二轮回溯复审（39 项修复，362 项测试全绿）
    ├── 前端人员A完成记录.md             # Android 前端人员 A 完成详情
    └── 前端人员B完成记录.md             # Android 前端人员 B 完成详情
```

---

## 文档维护约定

- **设计文档**（架构、API）在对应功能变更时同步更新
- **开发指南**（DEVELOPMENT、CONTRIBUTING）随工具链/流程调整而更新
- **实施记录**每完成一个阶段追加，格式参照 `阶段0完成记录.md`
- **命名**：核心永久文档用中文（便于团队阅读），工程规范文档沿用 Conventional 英文名（CONTRIBUTING/DEVELOPMENT 为社区通用文件名）
