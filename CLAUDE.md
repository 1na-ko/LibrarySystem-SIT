# CLAUDE.md — 图书馆智能管理系统 AI 开发指引

> **项目**: 图书馆智能管理系统 (LibrarySystem-SIT) — [README](README.md)
> **状态**: 阶段 0-4 ✅ | 阶段 5-11 📋 待实施
> **最后更新**: 2026-06-16

---

## 1. 项目概述

高校图书馆智能管理系统，集**基础图书管理** + **学科知识图谱** + **智能采编**三位一体。

- **后端**: Spring Boot 3.5 + MyBatis-Plus 3.5，Maven 多模块（Modular Monolith）
- **前端 (Android)**: Java 17 + Gradle Kotlin DSL，Material Design 3 + Hilt + Retrofit
- **目标等级**: 提高版（含知识图谱 + 智能采编）

完整架构设计见 [`docs/系统架构设计文档.md`](docs/系统架构设计文档.md)。

---

## 2. 技术栈速查

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.0 | 核心框架 |
| MyBatis-Plus | 3.5.5 | ORM |
| MySQL | 8.0.35 | 关系存储 |
| Redis | 7.2 | 缓存/分布式锁/预约队列 |
| Elasticsearch | 8.11.0 | 全文搜索 |
| Neo4j | 5.17.0 | 知识图谱 |
| RabbitMQ | 3.12 | 异步消息（待引入 Starter） |
| Flyway | 9.22.3 | 数据库迁移 |
| JJWT | 0.12.5 | JWT 令牌 |
| SpringDoc | 2.6.0 | OpenAPI 文档 |
| HanLP | portable-1.8.5 | 中文分词（本地轻量） |
| Commons Math | 3.6.1 | OLS 回归（简化 ARIMA） |
| DeepSeek API | - | LLM（NER/RE/谈判/推荐理由） |
| 阿里云百炼 | - | Embedding 向量化 |

---

## 3. 项目结构

```
LibrarySystem-SIT/
├── docs/                          # 📄 项目文档（索引 → README.md）
│   ├── README.md                  #   文档导航
│   ├── 系统架构设计文档.md          #   设计蓝本
│   ├── DEVELOPMENT.md             #   开发环境指南
│   ├── CONTRIBUTING.md            #   编码规范与协作
│   ├── api/library-api.yaml       #   OpenAPI 契约
│   ├── db/init.sql                #   Docker MySQL 初始化
│   └── implementation/            #   实施计划与进度记录
├── library-server/                # ☕ 后端 Maven 多模块项目（7 子模块）
│   ├── pom.xml                    #   父 POM — 统一版本与插件管理
│   ├── library-common/            #   📦 公共基础设施（异常/Result/DTO/工具）
│   ├── library-ai/                #   📦 AI 基础设施（LLM/Embedding/NLP）
│   ├── library-core/              #   📦 核心业务（图书/借阅/预约/推荐）
│   ├── library-knowledge-graph/   #   📦 知识图谱（Neo4j 主题网络/溯源）
│   ├── library-acquisition/       #   📦 智能采编（预测/查重/谈判）
│   ├── library-security/          #   📦 安全模块（JWT 过滤器/Spring Security）
│   └── library-bootstrap/         #   🚀 启动聚合（入口/配置/Flyway 迁移）
├── library-android/               # 📱 Android 前端（独立 Gradle 项目）
├── docker-compose.yml             # 🐳 中间件一键编排
├── .editorconfig                  # 📝 跨编辑器代码风格
└── .gitattributes                 # 🔤 强制 LF 行尾
```

### 模块依赖链

```
library-common ←── (所有模块的基础依赖)
library-ai     ←── library-core, library-acquisition, library-knowledge-graph
library-core   ←── library-knowledge-graph, library-acquisition, library-security
library-security ←── library-bootstrap
library-bootstrap ←── (聚合所有模块)
```

---

## 4. 常用命令

### 后端

```bash
cd library-server

# 编译（跳过测试）
mvn clean compile -DskipTests

# 运行测试
mvn test

# 启动（开发环境）
mvn spring-boot:run -pl library-bootstrap -Dspring-boot.run.profiles=dev

# 打包
mvn clean package -DskipTests

# 依赖树
mvn dependency:tree -pl library-core
```

### Docker 中间件

```bash
# 一键启动全部中间件
docker-compose up -d

# 首次安装 IK 分词器后需重启 ES
docker-compose restart elasticsearch

# 查看状态
docker-compose ps
```

### 健康检查

```bash
# 服务健康（所有组件应返回 UP）
curl http://localhost:8080/api/v1/health

# Swagger UI
open http://localhost:8080/api/v1/swagger-ui.html
```

---

## 5. 关键设计决策

1. **Modular Monolith** — 非微服务；按领域边界拆模块，通过 Spring Events 通信，未来可按需拆分
2. **Flyway 管理 DDL** — `V1__init_schema.sql` 为基线，`V2__*.sql`/`V3__*.sql` 按 feature 分支追加；`docs/db/init.sql` 仅做 Docker 首启字符集设置
3. **全局逻辑删除** — 所有业务表含 `deleted TINYINT NOT NULL DEFAULT 0`，MyBatis-Plus `logic-delete-field: deleted` 全局配置
4. **LLM 降级策略** — 所有 DeepSeek API 调用含降级路径（API 不可用时回退至本地模板/规则）
5. **ES 最终一致性** — MySQL 为主存储，ES 为搜索从存储，通过 Spring Events 异步同步（< 1s 延迟）
6. **`server.servlet.context-path: /api/v1`** — 全局路径前缀，Actuator `base-path: /` 使健康检查位于 `/api/v1/health`
7. **环境变量** — `.env.example` 模板，实际 `.env` 不入库；`application.yml` 通过 `${VAR:默认值}` 读取

---

## 6. 当前进度与约定

### 已落地
- Maven 7 模块结构 + 父 POM 统一版本管理 ✅
- Flyway V1 基线（10 张核心业务表）✅
- `library-bootstrap` 启动类 + 4 个环境配置文件 ✅
- logback-spring.xml 日志配置 ✅
- Android Gradle 项目骨架（4 Fragment + 导航图）✅
- Docker Compose 5 中间件编排 + IK 安装器 ✅
- `.editorconfig` + `.gitattributes` 跨平台代码风格 ✅

### 已落地（阶段 1：安全与认证）
- JWT 认证（HS256，Access 2h 无状态 / Refresh 7d 存 Redis 轮换防重放）✅
- Spring Security 6 配置 + `JwtAuthenticationFilter` + `RateLimitFilter` ✅
- Redis Lua 令牌桶限流（认证 100/min·用户，登录注册 20/min·IP 防爆破）✅
- RBAC 注解 `@RequireRole`/`@RequirePermission` + AOP 切面（KG Admin = LIBRARIAN + `kg:admin`）✅
- 认证四端点（register/login/refresh/logout）+ BCrypt(12) ✅
- `library-security` 模块 62 项单元测试全绿 ✅
- 初始管理员种子（admin/Admin@123456，V4 迁移）✅

### 已落地（阶段 2：核心业务数据层）
- 5 张核心表 Entity + Mapper：Category / Book / BorrowRecord / Reservation / FineRecord ✅
- 2 个新增枚举：BorrowStatusEnum / ReservationStatusEnum ✅
- 7 个 VO：CategoryVO(树形) / BookSimpleVO / BookDetailVO / BorrowRecordVO / ReservationVO / UserManageVO(脱敏) / UpdateUserDTO ✅
- CategoryService：getTree() O(n) 内存组装 / listByParentId() / getById() ✅
- BookService：getById() 含分类名联查 / getByIsbn() / listByIds() 批量 ✅
- UserService：getProfile() 无密码泄露 / updateProfile() / getManageVO() 脱敏 ✅
- CategoryController：3 个端点（GET /categories/tree / /categories / /categories/{id}）✅
- `library-core` 模块 16 项单元测试全绿 ✅
- 阶段间交叉审计修复：GlobalExceptionHandler 全量 37 个 ErrorCode→HTTP 映射修正 / CORS 路径修正 / @NoAuth 文档标注 ✅

### 已落地（阶段 3：核心业务—图书检索）
- ES 客户端手动配置 `ElasticsearchClient` Bean（elasticsearch-java 8.11 + JacksonJsonpMapper）✅
- ES 索引幂等初始化器 `EsIndexInitializer`（IK 分词 + Completion Suggester）✅
- `BookDocument` ES 文档模型 + `BookESRepository`（全文搜索/高级搜索/自动补全/热门图书）✅
- `BookSearchService`：Redis 缓存热点词（TTL 30min）+ ES 搜索 + 回写缓存 ✅
- `RelatedBookService`：MySQL 同分类/同作者降级实现（KG 就绪后替换）✅
- `BookController`：6 个端点（search / search/advanced / suggest / hot / {id} / {id}/related）✅
- `AdminBookController`：3 个端点（POST/PUT/DELETE /admin/books），含乐观锁 + 事件发布 ✅
- 领域事件：`BookCreatedEvent` / `BookUpdatedEvent` / `BookDeletedEvent`（Record）✅
- `ESSyncListener`：`@Async @EventListener` 异步同步 MySQL → ES（3 次重试）✅
- `BookDetailVO` 增强：keywordList（JSON 数组序列化）/ relatedBooks / reservationCount ✅
- `BookRecommendVO` + `SuggestVO` 视图对象 ✅
- 全量 254 项测试全绿（common 142 + core 40 + security 71 + bootstrap 1）✅

### 已落地（阶段 4：核心业务—借阅与预约）
- **借阅管理**：`BorrowService` 7 步校验链（用户状态→库存→上限→重复→超期→Redis 锁→乐观锁扣库存）✅
- **还书管理**：自动计算超期天数和罚款（0.5 元/日），生成 `FineRecord`，发布 `BookReturnedEvent` ✅
- **续借管理**：三重校验（次数<1、未超期、未被预约），延长 30 天 ✅
- **预约排队**：Redis ZSET `reservation:queue:{bookId}` 按时间戳公平排队，实时查询排队位置 ✅
- **预约通知**：`ReservationNotifier` `@Async @EventListener` 归还后自动通知队首读者（48h 确认窗口）✅
- **超期检查**：`OverdueCheckJob` `@Scheduled` 每天凌晨 3:00 扫描 overdue → 自动生成罚款 ✅
- **个人中心**：`GET /users/me` / `PUT /users/me` / `GET /users/me/history` / `GET /users/me/stats` ✅
- **`UserStatsService`**：聚合统计（分类分布饼图 + 近 12 月趋势）✅
- 领域事件：`BookBorrowedEvent` / `BookReturnedEvent`（Record）+ `ESSyncListener` 增强 ✅
- 5 个新增 DTO/VO：`BorrowRequest` / `ReservationRequest` / `BorrowResultVO` / `RenewResultVO` / `UserStatsVO` ✅
- `BorrowController`（6 端点）/ `ReservationController`（4 端点）/ `AdminBorrowController`（@RequireRole）/ `UserCenterController` ✅
- 8 模块 BUILD SUCCESS ✅ · 全量 280+ 项测试全绿 ✅
- 阶段 4 审计修复：还书/续借/详情增加归属校验（防横向越权）· `GlobalExceptionHandler` 新增 `BindException`/`MissingServletRequestParameterException`/`HttpMessageNotReadableException` 3 个 handler · `UserCenterController` 分层重构至 `UserStatsService` · API 路径对齐 OpenAPI 契约（`/borrows` / `/reservations`）✅

### 待实现
- 推荐引擎 / AI 基础设施 / 知识图谱 / 智能采编 的 Service/Controller
- 各中间件 Starter 引入（ES/Neo4j/RabbitMQ 的 auto-configuration）— ES 已通过手动配置启用
- 测试种子数据（`db/test-data/`）
- CI/CD 流水线

### 编码约定
- **Commit**: [Conventional Commits](https://www.conventionalcommits.org/)，中文 subject
- **分支**: `feature/<模块>-<简述>` / `fix/<模块>-<简述>`，Squash Merge → develop
- **Java 编码**: 阿里巴巴 Java 开发手册 + 项目 `docs/CONTRIBUTING.md` 补充
- **测试方法命名**: `should{预期行为}When{条件/输入}`
- **禁止**: Controller 直接调 Mapper、拼接 SQL、吞异常、push --force 到 main

---

## 7. 给 AI 助手的提示

- **源码按阶段实施** — 严格按 `docs/implementation/后端分阶段实施计划.md` 的顺序与任务清单推进，每阶段完成需编写完成记录归档至 `docs/implementation/`
- **每次操作同步文档** — 任何代码变更（新增 API、修改表结构、调整配置项、变更模块依赖）都必须同步更新对应的文档：架构设计文档、OpenAPI 契约、CLAUDE.md（进度状态）、以及涉及到的开发/贡献指南。代码与文档不一致视为未完成
- **文档优先** — `docs/系统架构设计文档.md` 是开发蓝本，优先以文档为准
- **OpenAPI 契约** — `docs/api/library-api.yaml` 是前后端数据契约，修改 API 需同步更新
- **健康检查路径** — `/api/v1/health`（非 `/actuator/health`）
- **配置文件注释** — `application.yml` 中对暂未生效的配置项有详细说明（ES/RabbitMQ 等待引入 Starter）
- **环境变量注入** — `.env` 文件仅作本地覆盖，所有配置键在 `application.yml` 中已有 `${VAR:默认值}` 默认值
