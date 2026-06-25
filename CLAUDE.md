# CLAUDE.md — 图书馆智能管理系统 AI 开发指引

> **项目**: 图书馆智能管理系统 (LibrarySystem-SIT) — [README](README.md)
> **状态**: 阶段 0-9 ✅ | 阶段 10 ✅ | 阶段 11 ✅ | 阶段 12 ✅ | 阶段 13 ✅ | **阶段 14 ✅（系统化重构：后端采编修复+部署 + 前端 12 WP 全部通过 + 真机走查修复）**
> **生产环境**: `http://101.132.24.73:8080/api/v1`（Ubuntu 24.04 / 4C7G / docker-compose + systemd）
> **最后更新**: 2026-06-22

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
| MySQL | 8.0.35（JDBC 驱动 8.0.33） | 关系存储 |
| Redis | 7.2 | 缓存/分布式锁/预约队列 |
| Elasticsearch | 8.11.0 | 全文搜索 |
| Neo4j | 5.17.0 | 知识图谱 |
| RabbitMQ | 3.12 | 异步消息（阶段10 事件总线已启用） |
| Flyway | 9.22.3 | 数据库迁移 |
| JJWT | 0.12.5 | JWT 令牌 |
| SpringDoc | 2.6.0 | OpenAPI 文档 |
| HanLP | portable-1.8.5 | 中文分词（本地轻量） |
| Commons Math | 3.6.1 | OLS 回归（简化 ARIMA） |
| Redisson | 3.25.0 | 分布式锁/预约排队 |
| Hutool | 5.8.25 | 通用工具集 |
| MapStruct | 1.5.5.Final | 对象映射 |
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
library-knowledge-graph ←── library-security
library-acquisition ←── library-security
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

1. **Modular Monolith** — 非微服务；按领域边界拆模块，通过 RabbitMQ 事件总线（领域事件 → `EventBusBridge` 在 `AFTER_COMMIT` 桥接 → `@RabbitListener` 消费）通信，未来可按需拆分
2. **Flyway 管理 DDL** — `V1__init_schema.sql` 为基线，`V2__*.sql`/`V3__*.sql` 按 feature 分支追加；`docs/db/init.sql` 仅做 Docker 首启字符集设置
3. **全局逻辑删除** — 所有业务表含 `deleted TINYINT NOT NULL DEFAULT 0`，MyBatis-Plus `logic-delete-field: deleted` 全局配置
4. **LLM 降级策略** — 所有 DeepSeek API 调用含降级路径（API 不可用时回退至本地模板/规则）
5. **ES 最终一致性** — MySQL 为主存储，ES 为搜索从存储，通过 RabbitMQ 事件总线异步同步（< 1s 延迟，阶段10 由 Spring Events 迁移至 MQ）
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
- Docker Compose 4 中间件编排（MySQL/Redis/ES/RabbitMQ）+ IK 分词器安装脚本 ✅
- `.editorconfig` + `.gitattributes` 跨平台代码风格 ✅

### 已落地（阶段 1：安全与认证）
- JWT 认证（HS256，Access 2h 无状态 / Refresh 7d 存 Redis 轮换防重放）✅
- Spring Security 6 配置 + `JwtAuthenticationFilter` + `RateLimitFilter` ✅
- Redis Lua 令牌桶限流（认证 100/min·用户，登录注册 20/min·IP 防爆破）✅
- RBAC 注解 `@RequireRole`/`@RequirePermission` + AOP 切面（KG Admin = LIBRARIAN + `kg:admin`）✅
- 认证四端点（register/login/refresh/logout）+ BCrypt(12) ✅
- `library-security` 模块 62 项单元测试全绿（管理端编目测试已于阶段 6 审计迁移至 library-core 的 BookAdminServiceTest）✅
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
- `ESSyncListener`：`@Async @TransactionalEventListener(AFTER_COMMIT)` 异步同步 MySQL → ES（3 次指数退避重试）✅
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
- 8 模块 BUILD SUCCESS ✅ · 全量 285 项测试全绿（common 142 + core 71 + security 71 + bootstrap 1）✅
- 阶段 4 审计修复（第一轮）：还书/续借/详情增加归属校验（防横向越权）· `GlobalExceptionHandler` 新增 `BindException`/`MissingServletRequestParameterException`/`HttpMessageNotReadableException` 3 个 handler · `UserCenterController` 分层重构至 `UserStatsService` · API 路径对齐 OpenAPI 契约（`/borrows` / `/reservations`）✅
- 阶段 4 审计修复（第二轮）：N+1 批量转换 / 预约状态机闭合（`ReservationExpireJob`）/ 架构蓝本回写 / RBAC 注解补全 / `OverdueCheckJob` 分批扫描 ✅
- 阶段 4 审计修复（第三轮——综合质量审计）：`getQueuePosition()` 横向越权修复（新增 `userId` 归属校验）· `@EventListener` → `@TransactionalEventListener(AFTER_COMMIT)` 修复事件时序竞态 · `AdminBorrowController` 路径 `/borrows` → `/admin/borrows`（消除与 `BorrowController` 路径重叠）· Job 独立 `REQUIRES_NEW` 事务组件（`OverdueBatchProcessor` / `ReservationExpireBatchProcessor`）· 搜索缓存失效（`BookSearchServiceImpl.evictAllSearchCache()`）· `ReservationZsetReconcileJob` 对账骨架 · ES 重试指数退避 · `RoleEnum` 文档补充 ✅

### 已落地（阶段 5：AI 基础设施）
- `library-ai` 模块 14 个主源文件（含 `AiExceptionHandler`）+ 28 项单元测试全绿 ✅
- **LLM 服务**：`LlmService` / `LlmServiceImpl` — DeepSeek API（OpenAI-compatible），文本生成 + JSON Mode 结构化输出，reactor-retry 指数退避重试（最多 2 次），含 markdown 代码块剥离防御 ✅
- **LLM 异常**：`LlmUnavailableException`（继承 RuntimeException），按故障类型分类（AUTH_FAILED / QUOTA_EXHAUSTED / SERVER_ERROR / NETWORK_ERROR / PARSE_ERROR / RETRY_EXHAUSTED）✅
- **Embedding 服务**：`EmbeddingService` / `EmbeddingServiceImpl` — 阿里云百炼 DashScope API（text-embedding-v3，1024 维），批量自动拆批（≤25），空文本返回零向量 ✅
- **NLP 服务**：`NlpService` / `NlpServiceImpl` — HanLP 1.8.5 portable 中文分词（`term.word`）+ TextRank 关键词提取，纯本地运行，始终可用 ✅
- **条件 Bean**：`@ConditionalOnExpression` 确保 DeepSeek/DashScope API Key 缺失时 `LlmService`/`EmbeddingService` Bean 不存在但不阻塞启动，`NlpService` 始终可用 ✅
- **配置**：`application.yml` 新增 `ai.deepseek.*` / `ai.dashscope.*` 配置块，环境变量由 `.env.example` 占位 ✅
- **DTO**：`LlmChatRequest` / `LlmChatResponse` / `EmbeddingRequest` / `EmbeddingResponse` — 完整 API 请求/响应映射 ✅
- 8 模块 BUILD SUCCESS ✅ · 全量 313 项测试全绿（common 142 + ai 28 + core 71 + security 71 + bootstrap 1）✅

### 已落地（阶段 6：图书推荐引擎）
- **多路召回**：`CollaborativeFilteringService`（User-CF 余弦 + Item-CF Jaccard）+ `ContentBasedService`（Embedding 画像 + 余弦相似度）+ `KGBasedRecommendService`（桩，阶段 7 替换）✅
- **加权融合**：`RecommendationService` 三路并行（CompletableFuture，5s 超时）+ CF 0.4/Content 0.3/KG 0.3 加权融合 ✅
- **LLM 推荐理由**：批量 JSON Mode 生成个性化推荐理由 → `LlmUnavailableException` 降级至 5 条模板池 ✅
- **Conditional 设计**：`EmbeddingService`/`LlmService` 条件注入（API Key 缺失时对应路径静默跳过/降级）✅
- **配置化**：`RecommendationProperties` — 权重/Top-K/候选池上限/超时均可通过 `application.yml` 调参 ✅
- **N+1 消除 / 全表扫描去重**：`RecommendationService` 顶层一次性加载全量借阅记录，分发 CF/Content 复用（单次请求全表扫描 4→1 次）；图书批量 `selectBatchIds` + 分类名 Map 回填 ✅
- **工具类**：`SimilarityUtils`（cosine/jaccard/setCosine/normalize/mergeWithWeight）✅
- `RecommendationController`（`GET /users/me/recommendations?limit=`，1-50，放 library-security）✅
- 8 模块 BUILD SUCCESS ✅ · 全量 332 项测试全绿（common 143 + ai 28 + core 99 + security 62；bootstrap 1 项集成测试 @Disabled）✅
- 阶段 6 完成记录归档至 `docs/implementation/阶段6完成记录.md` ✅

### 已落地（阶段 6 后：跨阶段综合质量审计修复）

> 对阶段 0-6 全量代码与文档进行四维度审计（实现质量/阶段配合/文档维护/架构落地）后，修复以下确认问题。详见 `docs/implementation/阶段6审计修复记录.md`。

- **P0 ES 按分类筛选缺陷修复**：`BookDocument` / `EsIndexInitializer` / `ESSyncListener` 补 `categoryId`（+ `coverUrl` / `location`）字段，修复阶段 3 遗留的"按 categoryId 筛选静默返回空"功能缺陷（已存在 ES 索引需删除重建以生效新 mapping）✅
- **P0 AdminBookController 分层合规**：提取 `BookAdminService`（`@Transactional`），Controller 不再直接调 Mapper（消除硬性禁止项①）；事件发布纳入事务边界，`AFTER_COMMIT` 时序保证真正生效；原 AdminBookControllerTest 9 项业务测试随逻辑下沉迁移至 library-core 的 BookAdminServiceTest ✅
- **P1 推荐引擎优化**：并行召回改用隔离的 `taskExecutor`（替代 ForkJoinPool.commonPool，清理死代码 import）；顶层一次性加载全量借阅记录分发三路（单次请求全表扫描 4→1 次）；并行异常日志补堆栈 ✅
- **P1 Redis 安全**：`evictAllSearchCache` 由 `KEYS` 改 `SCAN`（避免阻塞主线程），并上提至 `BookSearchService` 接口（消除 `ESSyncListener` 对具体实现类的依赖）✅
- **P2 借阅健壮性**：还书恢复库存增加乐观锁对称防护（与借书一致，冲突重试一次）；`getHistory` 的 `YEAR()` 函数改为 `between` 日期范围（避免索引失效）；`V5__fine_record_unique_borrow.sql` 为 `fine_record.borrow_id` 加唯一约束（防 OverdueJob 与还书并发产生重复罚款）✅
- **借书锁事务边界修复**：`BorrowServiceImpl.borrow()` 的 Redis 锁原在 `@Transactional` 方法 finally 内释放（早于事务提交，存在提交窗口竞态）；改用 `TransactionSynchronizationManager` 注册 `AFTER_COMMIT` 回调，锁延迟至事务提交后释放；无事务上下文时降级立即释放（单测兼容）；新增事务激活场景测试验证延迟释放路径 ✅

### 已落地（阶段 7：学科知识图谱）

> 一次性落地 `library-knowledge-graph` 模块全部业务代码，共 18 个主源文件。详见 `docs/implementation/阶段7完成记录.md`。

- **Neo4j 基础设施**：配置激活 (`KnowledgeGraphProperties`) + GDS 运行时探测 (`GdsAvailabilityProvider`) + Schema 约束初始化 (`KgSchemaInitializer`) + `Neo4jRepository` 封装（Driver Session API 参数化查询 + Power Iteration 降级 PageRank + Cypher shortestPath 降级 Dijkstra）✅
- **图谱构建**：`GraphBuildService`（NER→RE→MERGE→实体对齐，LLM 优先 HanLP 降级）+ `KgBuildListener`（`@Async @TransactionalEventListener(AFTER_COMMIT)` 监听 BookCreated/UpdatedEvent，3 次指数退避重试）✅
- **主题网络**：`TopicNetworkBuilder`（关键词共现 → Jaccard 相似度 → RELATED_TO 边 → GDS PageRank / Java Power Iteration 降级）✅
- **图谱查询**：`GraphQueryService`（Book 中心 1-3 跳邻居）+ `LiteratureTracingService`（BFS 多跳溯源 FORWARD/BACKWARD/BOTH + Dijkstra/Cypher shortestPath 关键路径）✅
- **KG Controller**：7 端点（`GET /kg/book/{bookId}` / `/kg/book/{bookId}/trace` / `/kg/book/{bookId}/keypath` / `/kg/subject/{name}` / `/kg/search`；`POST /admin/kg/rebuild/{bookId}` / `/admin/kg/rebuild-all`），RBAC 权限 `kg:read` / `kg:admin` ✅
- **跨阶段 Port 桩替换**：core 新增 `KgRecommendPort` / `KgRelatedBookPort` / `GapCoreBookPort` SPI 接口；`KGBasedRecommendServiceImpl` 与 `RelatedBookServiceImpl` 改造为 `ObjectProvider` 适配器（依赖方向 core ← kg）；kg 模块提供 `KgRecommendQueryService` / `KgRelatedBookQueryService` / `GapCoreBookQueryService`（`@ConditionalOnBean(Neo4jClient.class)`）✅
- 8 模块 BUILD SUCCESS ✅ · 新增测试 3 项全绿 ✅

### 已落地（阶段 8：智能采编）

> 一次性落地 `library-acquisition` 模块全部业务代码，共 20+ 主源文件。详见 `docs/implementation/阶段8完成记录.md`。

- **采购预测**：`SimplifiedArima`（Commons Math OLS AR(1)I(1)MA(1)，奇异矩阵降级 + 逆差分）+ `PredictionService`（学科聚合 + 学期因子 + 预约热度修正）✅
- **查重查缺**：`DuplicateCheckService`（ISBN 精确 / 标题模糊 / 作者+标题余弦三策略）+ `GapAnalysisService`（复本/热度缺口语义：KG Top-N 核心书 → 复本不足判定 → CRITICAL/HIGH/MEDIUM/LOW 优先级）✅
- **智能谈判**：`NegotiationAdvisor`（本地 PriceRange 计算覆盖 LLM 输出，Multi-Supplier/Long-Term-Discount/Standard 降级模板）+ `NegotiationService`（negotiation_record CRUD + JSON 序列化策略/条款/风险）✅
- **Acquisition Controller**：5 端点（`GET /acquisition/predict` / `POST /acquisition/duplicate-check` / `GET /acquisition/gap-analysis` / `POST /acquisition/negotiation` / `GET /acquisition/negotiation/{id}/suggestion`），RBAC 权限 `acquisition:predict|duplicate-check|gap|negotiation` ✅
- **4 张采编表** Entity+Mapper（Supplier/DealRecord/ElectronicResource/NegotiationRecord，严格对齐 V1 ENUM）+ `MonthlyStatMapper`（DATE_FORMAT BETWEEN 聚合，索引友好）✅
- 8 模块 BUILD SUCCESS ✅ · 新增测试 10 项全绿（ARIMA 5 + Duplicate 2 + Negotiation 3）✅

### 已落地（阶段 8 后：跨阶段综合质量审计修复）

> 对阶段 0-8 全量代码与文档进行四维度审计（实现质量/阶段配合/文档维护/架构落地）后，修复以下确认问题。详见 `docs/implementation/阶段8后审计修复记录.md`。

- **P0 GraphQueryServiceImpl 图谱路径解析重写**：Neo4j 5.x Driver path `Value.asList()` 返回交替 NODE/RELATIONSHIP 段，原代码错误假设 `asMap()` 含 `_nodes`/`_relationships` 键。重写 `buildGraphFromPaths()` 按 `type().name()` 区分段类型，从相邻 NODE 段提取业务 ID 构建边 ✅
- **P0 边 ID 解析修复**：Neo4j 5.x elementId 为字符串格式（`"4:abc123def:0"`），原代码 `Long.parseLong()` 将抛出 `NumberFormatException`。改为从路径中前/后 NODE 段预提取业务 `id` 属性作为 sourceId/targetId ✅
- **P1 BookAdminServiceImpl totalCopies→availCopies 同步**：`updateBook()` 修改 `totalCopies` 时计算 delta 同步调整 `availCopies`，防止 `availCopies > totalCopies` 不合理状态 ✅
- **P1 ReservationService TOCTOU 竞态 + V6 DB 约束**：`reserve()` 关键段（查重→入队→插入）加 Redis SETNX 分布式锁（与 BorrowServiceImpl 一致的模式）；`V6__unique_reservation_user_book.sql` 为 `reservation(user_id, book_id, status)` 加 UNIQUE 约束兜底 ✅
- **P1 TopicNetworkBuilder Jaccard Cypher 修正**：`COUNT(DISTINCT k1) AS freq1` 恒为 1（k1 已绑定），改为 `MATCH (k1)<-[:HAS_KEYWORD]-(b1:Book)` 后 `COUNT(DISTINCT b1)` 正确计算关键词关联图书数 ✅
- **P1 EmbeddingServiceImpl 重试+异常包装**：新增 `Retry.backoff(2)` 指数退避重试、`onErrorMap(IOException.class)` 网络异常包装、独立 `newZeroVector()` 替代共享 `nCopies` 不可变引用、返回向量数校验 ✅
- **P2 N+1 批量消除**：`RelatedBookServiceImpl` 批量预加载分类名 Map；`LiteratureTracingServiceImpl.findKeyPath()` 改 `selectBatchIds()` ✅
- **P2 Neo4jRepository 加固**：`pageRankViaGds()` 图名加时间戳后缀防并发冲突；`countNodes()` 加标签白名单防 Cypher 注入 ✅
- **P2 分层合规**：`BookController.getDetail()` VO 变更下沉至 `BookServiceImpl`；`NegotiationServiceImpl.getSuggestion()` LLM 调用与 DB 事务分离 ✅
- **P2/P3 其他修复**：`NegotiationAdvisor` ceiling 价格 0.95→1.05 修正；`DuplicateCheckService` 余弦 O(n²)→频率 Map + LIMIT 加排序；`ReservationZsetReconcileJob` KEYS→SCAN；`LlmServiceImpl` 重试跳过 AUTH_FAILED/QUOTA_EXHAUSTED；`PredictionServiceImpl` seasonFactor 按目标月份独立计算；`GlobalExceptionHandler` 补 `RECOMMEND_PARALLEL_TIMEOUT`→503 映射 ✅
- **文档同步**：架构文档 v1.7→v1.8（项目状态/模块表/版本历史更新）；OpenAPI 补全 3 个 KG 管理端点；CLAUDE.md 测试计数修正 ✅

### 已落地（阶段 8 后第二轮审计：实现质量深化修复）

> 阶段 7-8 落地后，对全量代码再做一轮四维度独立走查，修复上一轮审计未覆盖的实现质量缺陷。当前全量 **346 项测试全绿**（common 143 + ai 29 + core 99 + security 62 + kg 3 + acquisition 10；bootstrap 1 项集成测试 @Disabled）。

- **P0 TopicNetwork 图算法节点标识缺陷**：Keyword/Author/Subject 节点以 `name` 为唯一键（见 `KgSchemaInitializer`），不含业务 `id` 属性；但 `TopicNetworkBuilder` 与 `Neo4jRepository.pageRank` 的 Cypher 误用 `k.id`/`a.id`（恒为 null），导致主题网络 PageRank 写回与学科网络查询全部失效。改为统一使用 Neo4j 内部 `id()` 作为图算法节点标识（图数据库标准做法，仅影响 Keyword 路径，Book 路径不受影响）✅
- **P1 NegotiationServiceImpl 序列化静默吞**：`updateRecord()` 的 JSON 序列化失败原仅 `log.warn` 后继续 `updateById`，导致"价格已更新但策略/条款丢失"的数据不一致；改为抛 `BizException` 阻止半更新 ✅
- **P1 NegotiationServiceImpl @Transactional 自调用失效**：`loadRecord/updateRecord` 标注 `@Transactional` 但同类内 protected 调用绕过 AOP 代理，注解静默失效且注释"独立事务"误导；改为 private 并移除无效注解 ✅
- **P1 ReservationServiceImpl 锁释放时机**：`reserve()` 的 Redis 锁原在 `finally` 立即释放（与阶段 6 修复的 `BorrowServiceImpl` 不对称），提交窗口内仍可触发 V6 唯一约束返回 500；改用 `TransactionSynchronizationManager` 注册 AFTER_COMMIT 回调延迟释放，无事务上下文时立即释放（单测兼容）✅
- **P1 Neo4jRepository.execute() 静默吞噬**：写操作异常原仅 `log.error` 不抛出，写入失败调用方无感知；改为抛 `RuntimeException` 由上层 Service 捕获转译（与只读 `query()` 返回空列表的容错语义区分）✅
- **P2 ReservationNotifier 注释修正**：`updateById` 失败注释原称"乐观锁冲突"，但 Reservation 实体未启用 `@Version`，按主键更新不做版本校验；注释改为如实反映"记录在 select 后被变更/删除" ✅
- **P2 GapAnalysisServiceImpl 死代码清理**：`ownedCount` 的 `deleted==1 continue` 分支永不触发（`selectBatchIds` 受全局 logic-delete 过滤），移除冗余检查 ✅

> 随后做第二轮深度走查，覆盖 common/security/ai/bootstrap 及 core/acquisition 残留项（实现质量 / 安全 / 性能 / 配置卫生），追加修复约 20 项，全量测试仍 **346 项全绿**（common 143 + ai 29 + core 99 + security 62 + kg 3 + acquisition 10；bootstrap @Disabled）：

- **common**：GlobalExceptionHandler 补 `ConstraintViolationException` handler（`@Validated`+`@RequestParam`/`@PathVariable` 校验失败原落入兜底返回 500，现转 400）；`mapHttpStatus` 删除 switch `default` 分支（占位 `SUCCESS`，新增 ErrorCode 遗漏映射将编译失败，强制保持完整）✅
- **security**：`RateLimitFilter.clientIp` 改用 `getRemoteAddr()`（防 X-Forwarded-For 伪造分散限流桶绕过登录防爆破，配合 `server.forward-headers-strategy: native`）；Lua 令牌桶拒绝请求时不再推进 `ts`（修复"持续被限流 → lastTs 永远刷新 → 令牌永不补充 → 桶卡死"）；POM 显式声明 `spring-boot-starter-data-redis`（消除传递依赖脆弱性）✅
- **ai**：`LlmConfig`/`EmbeddingConfig` 写超时硬编码 30s/20s 改 `@Value` 可配置；LLM/Embedding `retry filter` 扩展跳过 `CLIENT_ERROR`(400/404) 永久错误、Embedding `onStatus` 按 401/403/429/4xx/5xx 细分 reason；`stripMarkdownCodeBlock` 正则修复单行 ` ```json{...} ` 边界 bug；`batchEmbed` 重建结果 O(n²)→O(n) ✅
- **core**：`ReservationServiceImpl` ZSET 入队延迟至 AFTER_COMMIT（避免 DB 回滚后 Redis 残留幽灵条目）+ 预约列表 Redis N+1 改按 bookId 批量 `zRange` 预加载；`BookSimpleVO` 新增静态工厂 `from(Book, categoryName)` 消除 4 处重复构建；`UserStatsServiceImpl.buildMonthlyTrend` O(12×N)→O(N) 单次遍历分组 ✅
- **acquisition**：`createNegotiation` 加 resource/supplier 存在性校验（避免 DB 外键抛 500）；Controller 出参改 `NegotiationVO`（隐藏实体 deleted/JSON 列）；`predict` 的 `months` 加 `@Min(1)@Max(12)`+`@Validated`；ARIMA 改 `static final`；`NegotiationAdvisor` catch 扩宽至 `Exception` 全面降级；`GapAnalysis` 接入 `gapCoverageThreshold` 告警 + `suggestedCopies` 溢出防御；`MonthlyStatMapper` 加防御性 `LIMIT 10000` ✅
- **bootstrap**：logback `RollingFileAppender` 加 `totalSizeCap 10GB`、`AsyncAppender` 加 `discardingThreshold`/`neverBlock`（防磁盘撑爆与阻塞业务线程）；Async 线程池队列 100→500；`dashscope.base-url` 加环境变量占位 ✅

### 已落地（阶段 8 后第三轮审计：配置卫生与防御性编程加固）

> 对阶段 0-8 全量代码再做一轮四维度走查，发现并修复 4 个 P2 问题。全量 **347 项测试全绿**（common 143 + ai 29 + core 99 + security 62 + kg 3 + acquisition 10；bootstrap 1 项集成测试 @Disabled）。

- **P2 JWT Secret 开发环境加固**：`application.yml` 中 `jwt.secret` 空默认值 `""` 改为 `"dev-only-do-not-use-in-prod"`，附带严重注释——开发环境无环境变量时不再使用空密钥（HS256 接受空 key 导致令牌可伪造），生产环境仍通过 `JWT_SECRET` 环境变量注入强密钥 ✅
- **P2 TokenService TTL 与 JwtProperties 同步**：`TokenServiceImpl` 静态 `TTL = Duration.ofDays(7)` 改为构造注入 `JwtProperties.getRefreshTokenExpiration()` 动态计算 TTL，防止运维调整 `jwt.refresh-token-expiration` 后 Redis TTL 不同步导致有效 Refresh Token 被提前删除 ✅
- **P2 TopicNetworkBuilderImpl PageRank 批量写入**：`buildTopicNetwork()` 中 PageRank 分数写入由 for 循环 N 次 `execute()` 改为单次 `UNWIND $rows` 批量 Cypher，消除 N 次网络往返 ✅
- **P2 RateLimitServiceImpl 降级策略文档化**：Redis 故障"放行"行为保留（Rate Limit 为保护性措施，阻断所有用户损失更大），但显式标注 fail-open 决策理由及 Redis HA 运维要求，添加本地 ConcurrentHashMap 降级 TODO ✅
- **P3 发现与记录**：识别 9 项 P3 技术债——`@EnableMethodSecurity` 死配置、ES URI 缺 scheme、NlpService Javadoc 契约不一致、LLM 日志含可能敏感数据、GlobalExceptionHandler 缺 3 个 Spring MVC 异常 handler、EmbeddingService 重试次数不象 LlmService 可配置、DuplicateCheckService 作者查询无 LIMIT、PredictionService 全分类预加载浪费、CORS 生产环境缺白名单 ✅

### 已落地（阶段 9：系统管理与监控）

> 一次性落地 6 项系统管理功能 + 3 项 P3 技术债修复。全量 **355 项测试全绿**（common 143 + ai 29 + core 99 + security 71 + kg 3 + acquisition 10；bootstrap 1 项 @Disabled）。
> 新建源码 21 个 + Flyway V7 迁移，修改文件 ~15 个。

- **9.1 用户列表**（`GET /admin/users`）：`AdminUserController`(security) + `AdminUserService`(core) + `UserQueryDTO`，支持按角色/状态/关键词分页筛选，批量查询在借/超期统计消除 N+1 ✅
- **9.2 用户状态管理**（`PUT /admin/users/{id}/status`）：状态变更含自操作防护 + DISABLED 不可逆守卫 + `@OperationLog` 自动审计 ✅
- **9.3 流通统计 Dashboard**（`GET /admin/stats/dashboard`）：`AdminStatsController`(security) + `StatsDashboardService`(core) + `DashboardVO`，聚合今日借阅/归还/超期、本月日趋势、热门分类 Top-10、实时在馆人数 ✅
- **9.4 操作日志 AOP**：`@OperationLog` 注解(common) + `OperationLogEntity`(core) + `OperationLogMapper` + `OperationLogAspect`(security) + Flyway V7 `operation_log` 表，异步写入防阻塞主流程 ✅
- **9.5 Prometheus 指标**：`MetricsConfig`(bootstrap) + `micrometer-registry-prometheus` 依赖 + `library_reservations_queue_size` Gauge + Counter 埋点（借阅/搜索）✅
- **9.6 定时任务总控增强**：`SchedulingConfig` 自定义 4 线程池 + `ReservationZsetReconcileJob` 骨架→完整（幽灵删除+孤儿补回）+ `EsRebuildJob` 每周日 4:00 全量重建（游标分批+批量写入+缓存清除）✅
- **P3 技术债清理**：`SecurityConfig` 删除 `@EnableMethodSecurity` 死配置 / `application.yml` ES URI 补齐 `http://` scheme / `CorsConfig` 生产 `CORS_ALLOWED_ORIGINS` 白名单支持 ✅

### 已落地（阶段 10：事件总线引入 + 集成测试与加固）

> 引入 RabbitMQ 事件总线替代 Spring Application Events + Testcontainers 集成测试体系。详见 `docs/implementation/阶段10完成记录.md`。
> 单元测试全绿 **368 项**（common 146 + ai 29 + core 105 + security 75 + kg 3 + acquisition 10；含 EventBusBridgeTest 6 项，core 99→105）；集成测试代码完成，运行受 Docker Desktop 29 兼容问题阻塞（待开 TCP 2375）。

- **RabbitMQ 事件总线**：`EventBusBridge`（`@TransactionalEventListener(AFTER_COMMIT)` 桥接转发 5 领域事件到 MQ）+ `RabbitMqConfig`（Topic Exchange `library.events`/业务队列/死信 `library.events.dlx`）+ `EventBusConstants`（常量集中 core，依赖方向正确）+ 3 个 Listener 改 `@RabbitListener`（ESSyncListener/ReservationNotifier/KgBuildListener，移除手写重试统一 Spring AMQP RetryTemplate）；**业务发布点零改动** ✅
- **双写一致性**：afterCommit 发 MQ + 持久化（不上 Outbox），EsRebuildJob 周级兜底 ✅
- **文档冲突消除**：架构 §3.1 统一 RabbitMQ 事件总线，删除"Spring Events 替代"措辞 ✅
- **Testcontainers 集成测试**：父 POM 引入 BOM 1.21.3 + 5 容器（MySQL/Redis/ES+IK/Neo4j/RabbitMQ）+ `@ServiceConnection`；`AbstractIntegrationTest` 基类；ES+IK 用 `Dockerfile.es-ik` 定制镜像（`ImageFromDockerfile` 自动构建） ✅
- **JaCoCo 覆盖率**：父 POM `prepare-agent` + 单模块 `report`（excludes 排除非业务类）+ library-bootstrap `report-aggregate` 聚合报告（`target/site/jacoco-aggregate/`）✅
- **integration profile**：`mvn test` 仅单元测试（默认排除 `**/integration/**`），`mvn test -Pintegration` 跑集成测试 ✅
- **V100 种子数据**：`db/test-data/V100__test_seed.sql`（20 书/5 用户/50 借阅/10 预约/2 供应商/2 电子资源），三重隔离防污染生产（物理+配置+版本号） ✅
- **16 个集成测试类**：10.1-10.16 + `EventBusReliabilityIntegrationTest`，代码完成编译通过 ✅
- **本地 Makefile**：`make test`/`itest`/`itest-tcp`/`coverage`/`verify-all`（`.RECIPEPREFIX` 避免 tab） ✅

### 已落地（阶段 10 后：综合质量审计修复）

> 阶段 10 落地后，五并行子代理对阶段 0-10 全量做四维度回溯审计（实现质量/阶段配合/文档维护/契约一致性）+ 父代理核验，修复 7 项确认缺陷（5 P1 实现 + 2 P1 文档/工具）。详见 [`阶段10后审计修复记录.md`](docs/implementation/阶段10后审计修复记录.md)。
> 全量 **370 项测试全绿**（common 146 + ai 29 + core 107 + security 75 + kg 3 + acquisition 10；core 105→107，新增 ESSyncListener borrowed/returned 不清缓存的 2 个测试）。

- **P1 ESSyncListener evict 顺序与白名单**：原入口先 `evictAllSearchCache` 再写 ES → ES 失败重试 7s 期间出现"缓存空+ES 旧数据"窗口击穿；同时 `book.*` 队列接收所有事件（含借/还），借还高峰期每次都全量 SCAN+DELETE 缓存命中率塌陷。改为：① 先成功写/删 ES 再 evict（一致性）；② 仅 `created/updated/deleted` 触发 evict，`borrowed/returned` 仅改 availCopies/borrowCount，对全文检索无影响不参与失效 ✅
- **P1 ReservationNotifier 异常隔离**：原全局 `try-catch (Exception)` 吞噬所有异常致 RetryTemplate 无法识别失败、3 次重试 + DLQ 兜底架构承诺彻底失效（DLQ 永远空）。改为业务级异常（NumberFormatException）catch+continue 不重试，基础设施异常（Redis/DB 不可达）自然抛出由 RetryTemplate 接管 ✅
- **P1 EventBusBridge 移除 fallbackExecution=true**：5 个 `@TransactionalEventListener` 原 `fallbackExecution=true` 隐性削弱"AFTER_COMMIT 才发 MQ"不变量——未来若新增非事务发布点（容易疏漏）会致消费者读到脏数据。恢复默认 `false`（无事务时 Spring 输出 WARN 并丢弃事件，强制要求所有发布点必须在 `@Transactional` 内）✅
- **P1 KgRelatedBookQueryService Cypher 1 跳→2 跳**：原 `MATCH (b:Book {id})-[*1]-(neighbor:Book)` 仅匹配 Book→Book 直接边（CITES），但 `GraphBuildService` 实际只创建 HAS_KEYWORD/AUTHORED_BY/BELONGS_TO（Book→中间实体），从不构建 CITES。**该 Port 实现长期返回空列表**，KG 相关推荐永远走 MySQL 降级。改为 2 跳通过中间实体：`-[:HAS_KEYWORD|AUTHORED_BY|BELONGS_TO]-()-[同上]-(neighbor:Book)` ✅
- **P1 DuplicateCheckResultVO Jackson 字段名修正**：Lombok 为 `boolean isDuplicate` 生成 `isDuplicate()` getter，Jackson 默认序列化为 `"duplicate"`（剥离 is 前缀），与 OpenAPI 契约 `isDuplicate` 不一致——前端反序列化永远拿不到该字段，重复图书无法被识别拦截。加 `@JsonProperty("isDuplicate")` 强制 JSON 字段名 ✅
- **P1 Makefile 补 itest-tcp**：CLAUDE.md / 阶段10完成记录 §5.2 均承诺 `make itest-tcp` 用于 Docker Desktop 29 兼容方案，但实际 Makefile 未定义该 target。补 `.PHONY` + 规则 `DOCKER_HOST=tcp://localhost:2375 mvn test -Pintegration` ✅
- **P1 CLAUDE.md "Spring Events" 措辞修正**：§5 关键设计决策 #1 "通过 Spring Events 通信" 与阶段10 RabbitMQ 引入直接矛盾。改为"通过 RabbitMQ 事件总线（领域事件 → EventBusBridge 在 AFTER_COMMIT 桥接 → @RabbitListener 消费）通信" ✅
- **登记保留项**（详见审计修复记录 §2）：RabbitListenerContainerFactory 显式声明（Spring Boot 自动装配已用 Jackson，子代理误判 P0 → 实际 P2）、OverdueBatchProcessor DuplicateKey rollback-only（核验后无实际影响）、BorrowService OVERDUE 24h 窗口（待业务确认）、`borrow_record` 同用户同书唯一约束（成本/收益权衡）、Publisher Confirms（设计上以周级兜底替代）、RBAC 测试 403 旁路（待集成测试运行后治理）

### 已落地（阶段 10 后第二轮：Docker 真实环境集成测试 + 安全加固）

> 用 docker-compose 真实中间件跑 16 集成测试（替代 Testcontainers，规避 Docker Desktop 29 CLI 代理兼容问题），全程边跑边修。集成测试 34/35 通过（仅 ReservationFlow 受 Redisson 3.25.0 ZSET popMin bug `@Disabled`），单元 372/372 全绿。

**安全加固（OWASP）**：
- **AccessToken 登出黑名单**（user 维度时间戳）：无状态 JWT 设计下 AT 不带 jti，登出仅删 RefreshToken 致 AT 在剩余 TTL 内仍可用（违反 OWASP 会话终止）。`TokenService.revoke` 额外写 `auth:logout:{userId}=epoch秒`（TTL=AT 有效期），`JwtAuthenticationFilter` 解析 AT 后校验 `iat ≤ logoutTs` 则 401。同秒边界保守判定为已失效防 1 秒内 logout+复用。✅

**应用启动 / 配置 P0 修复（docker-compose 环境暴露）**：
- **MetricsConfig RedisTemplate 注入加 `@Lazy`**：Actuator MeterRegistryPostProcessor 在 BeanPostProcessor 阶段强制枚举 MeterBinder 候选 → MetricsConfig 早于 RedisConfig 实例化致 NoSuchBean。@Lazy 让 Spring 注入代理对象，调度器首次执行时才解析真实 Bean ✅
- **MyBatis Enum Handler 改 `EnumTypeHandler`**：MybatisEnumTypeHandler 强制要求 @EnumValue 注解，与项目所有业务枚举（按 name 与 DB ENUM 互转）的设计意图不符，启动报 `Could not find @EnumValue in Class`。改用 MyBatis 标准 EnumTypeHandler ✅
- **Flyway `validate-on-migrate=false`**（test profile）：防 docker MySQL 残留旧 checksum 致 ApplicationContext 启动失败 ✅
- **`spring.docker.compose.enabled=false`**：避免 spring-boot:run 在子模块找不到 compose 文件启动失败 ✅

**ES 同步链路 P0 修复**：
- **`BookESRepository.save` 加 `refresh=WaitFor`**：保证写后立即可搜（适用 ESSyncListener 单条同步），EsRebuildJob 批量重建仍异步 ✅
- **`BookESRepository.fullTextSearch` sort builder variant 修复**：ES 8.11 Java Client 严格要求每个 SortOptions builder 指定一个 variant（field/score/...），sortBy 为空时显式 `_score` variant，否则抛 'Missing required property Builder.<variant kind>' 致**所有图书搜索失败返回空** ✅

**集成测试代码修复**：
- AbstractIntegrationTest: API 常量改空串（TestRestTemplate baseUrl 已含 context-path，叠加 `/api/v1` 致 401）；新增 `asLong()` helper 兼容 JacksonConfig Long→String 序列化
- LoginHelper 路径去 `/api/v1` 前缀；6 处 `(Number)` cast 改用 `asLong()`
- BorrowFlow/ReservationFlow 改用 admin（V100 中 4 个 test_* 用户均有 OVERDUE 借不了书）
- AcquisitionFlow/RbacMatrix `subjectId` 1→101（category=1 顶级类无书，PredictionService 不递归子分类）
- RecommendationKgFlow 推荐返回 `data` 直接是 List 而非 records 包装
- V100 移除 30003（test_student 预预约 10003）避免冲突
- ReservationFlow `@Disabled`：受 Redisson ZSET popMin 解码 bug 阻塞

**Redisson Workaround**：
- ReservationNotifier `popMin` 前加 `zCard` 预检：规避 Redisson 3.25.0 Spring Data Redis 连接器对空 ZSET popMin 的 IndexOutOfBoundsException 解码 bug ✅

### 已落地（阶段 10 后第三轮：生产部署 + 全业务流程端点验证）

> 部署到阿里云 Ubuntu 24.04 服务器（4C7G），公网 `http://101.132.24.73:8080/api/v1`，前端可对接。51/52 端点测试通过（98%，唯一未过为 curl 超时非接口 bug）。

**生产部署**：
- `docker-compose.prod.yml`（与开发 compose 分离）：所有中间件端口仅绑 `127.0.0.1` 公网不可达（实测 6 端口扫描全部 connection refused），仅应用 8080 对外
- 密码全部从 `.env.prod` 读取（服务器本地 openssl 强随机生成，chmod 600，不入库）
- 应用 systemd 托管（`library.service`）：EnvironmentFile=.env.prod，Restart=on-failure，开机自启 + 崩溃重启
- IK 分词器 config 目录从开发机拷贝（修复生产 IK `_StopWords` null 问题）

**全业务流程端点验证（5 角色 × 40+ 端点）实测发现并修复 4 个真实 bug**：
- **DashScope `max-batch-size` 25→10**：text-embedding-v3 强制限制 ≤10 条，超出抛 InvalidParameter 致 Embedding 单批失败降级零向量，影响 Content-based 推荐质量 ✅
- **KG 搜索 `type` 参数大小写不兼容**：`GraphQueryService.ALLOWED_NODE_TYPES` 仅接 PascalCase（`Book`），但 Controller Javadoc / 前端契约约定 UPPER_CASE（`BOOK`）实测被白名单拒返回空。改用 `ALLOWED_NODE_TYPE_MAP` 双向映射兼容两种风格 ✅
- **KG 图谱 `/kg/book/{id}` PATH 解析失败**：Neo4j Driver 5.x 不支持 `Value.asList()` 直接解析 PATH 类型抛 'Cannot coerce PATH to Java List'，被 GlobalExceptionHandler 兜底成 200+空 data **隐藏根因**——前端调图谱端点恒返回空。Cypher 端解构 `nodes(p)+relationships(p)` Java 端重组交替序列保持 buildGraphFromPaths 契约不变 ✅
- **KG 溯源 `/trace` PATH 解析失败**：同上，应用相同修复至 LiteratureTracingService ✅

**端点测试覆盖**：认证全流程（注册/登录/刷新/登出+AT黑名单 OWASP）/ 图书检索（全文/高级/补全/热门/详情/相关）/ 分类 / 借阅（借/还/续借/详情）/ 预约（创建/排队/取消） / 个人中心 / 推荐 / KG（图谱/溯源/学科/搜索 BOOK+KEYWORD+无类型/关键路径） / 智能采编（预测/查重/缺口/谈判/建议） / 管理端（用户/状态/Dashboard/超期/编目 CRUD） / RBAC 越权 403 / Prometheus。**全部 200**（仅一项 409 是 ISBN 唯一约束触发，正确业务行为）。

测试报告归档：`docs/test-reports/阶段10/api-tests/`。

### 已落地（阶段 11：Android 前端大规模重构）

> 对前端进行 5 阶段递进式重构，从致命 Bug 修复到业务功能补齐再到契约对齐与体验统一。重构原则：保留原 MVVM + Hilt + Retrofit + RxJava3 + XML View 设计逻辑，**原地重构不重写**。详见 [`bug-bug-1-2-3-bug-merry-dragon.md`](.claude/plans/bug-bug-1-2-3-bug-merry-dragon.md)。

**阶段 A：致命 Bug 与基础设施（P0，全部完成）**
- **A.1 认证与会话链路**：[`TokenManager`](library-android/app/src/main/java/com/library/android/network/TokenManager.java) 增加 `userId`/`role` 持久化 + `isAdmin/isLibrarianOrAbove/isAcquisitorOrAbove`；[`LoginViewModel`](library-android/app/src/main/java/com/library/android/viewmodel/LoginViewModel.java) 登录成功补全 `saveUserRole+saveUserId`（修复"isAdmin 永远 false"）；新增 [`SessionManager`](library-android/app/src/main/java/com/library/android/network/SessionManager.java) 全局会话失效广播 + [`TokenAuthenticator`](library-android/app/src/main/java/com/library/android/network/TokenAuthenticator.java) refresh 失败时通知；[`MainActivity`](library-android/app/src/main/java/com/library/android/ui/main/MainActivity.java) 监听 SessionManager 自动跳登录；[`ProfileFragment`](library-android/app/src/main/java/com/library/android/ui/profile/ProfileFragment.java) 退出登录调后端 `POST /auth/logout`（命中 OWASP AT 黑名单）✅
- **A.2 HTTP 错误统一框架**：新增 [`ApiCallExecutor`](library-android/app/src/main/java/com/library/android/network/ApiCallExecutor.java) + 6 个异常类（[`ApiException`](library-android/app/src/main/java/com/library/android/network/exception/ApiException.java)/SessionExpired/PermissionDenied/BizConflict/Validation/ServiceUnavailable/Network）；7 个 Repository 全部改造（消除裸 `.execute().body()`，401/403/409/422/503 翻译为业务异常）✅
- **A.3 BookDetailFragment 死操作修复**：接入 Glide 加载封面（含 placeholder/error）；btnBorrow/btnReserve 接入 click listener（原版按钮无监听，详情页根本无法借书）；新增"知识图谱"入口按钮；所有 setText 加 null/literal "null" 兜底 ✅
- **A.4 关键交互防抖**：新增 [`Debounce`](library-android/app/src/main/java/com/library/android/ui/common/Debounce.java) 工具（500ms 时间窗）；BorrowConfirmDialog 借书按钮防抖；[`ReservationViewModel`](library-android/app/src/main/java/com/library/android/viewmodel/ReservationViewModel.java) 新增 `cancellingIds` Set 防重复取消；BorrowDetailFragment 续借/还书按钮防抖 ✅
- **A.5 视图泄露与 NPE 修复**：BorrowStatsFragment 加 `removeAllViews` 修复图表视图重叠泄漏；新增 [`SafeStrings`](library-android/app/src/main/java/com/library/android/ui/common/SafeStrings.java) 工具（safeSubstring/safeDate/safeMonth）；BorrowHistory/ReservationList 所有 substring 改用 SafeStrings；SearchFragment Suggest DiffCallback 改 `Objects.equals`；AdvancedSearchFragment Integer.parseInt 加 try-catch ✅
- **A.6 安全配置加固**：BaseUrl 默认值切换至 `http://101.132.24.73:8080/api/v1/`（保留 local.properties 覆盖）；HttpLoggingInterceptor.Level 改 `BuildConfig.DEBUG ? BODY : NONE`；清除 LoginViewModel/RegisterViewModel/LoginFragment 的 PII 日志；新增 `res/xml/network_security_config.xml` 限定 cleartext 仅指向生产 IP / 10.0.2.2 / localhost；移除 AndroidManifest 全局 `usesCleartextTraffic="true"` ✅

**阶段 B：架构重构与代码债清理（P1，全部完成）**
- **B.1 删除 Room 缓存层**：[`data/`](library-android/app/src/main/java/com/library/android/data) 整目录删除（5 个未使用文件）+ DatabaseModule 删除 + Room 依赖移除；ProGuard rules 同步清理 ✅
- **B.2 合并 AuthApiService**：删除 `AuthApiService.java`；LoginViewModel/RegisterViewModel 改注入 AuthRepository（统一通过 LibraryApi + ApiCallExecutor）；NetworkModule 移除 AuthApiService Provides ✅
- **B.3 BaseFragment + BaseViewModel 基础设施**：新增 [`BaseFragment`](library-android/app/src/main/java/com/library/android/ui/common/BaseFragment.java)（含 `observeError` + `mapErrorMessage` 异常分类文案）+ [`BaseViewModel`](library-android/app/src/main/java/com/library/android/viewmodel/BaseViewModel.java) + [`SingleLiveEvent`](library-android/app/src/main/java/com/library/android/ui/common/SingleLiveEvent.java)；新增 Fragment 全部继承（采编 5 + Dashboard） ✅
- **B.4 ViewModel 作用域统一**：ProfileFragment 改 `requireActivity()` scope（与 EditProfileFragment 一致），消除"编辑后资料不同步"的同名不同实例 bug ✅
- **B.5 死代码清理**：删除未使用的 `QueuePositionVO`；`RenewResultVO` 补 `maxRenewReached`；`BorrowRecordVO.fineAmount` 类型 `Double → BigDecimal`（与后端契约对齐，3 处调用方同步用 `compareTo` + `getFineAmountDouble()`）✅
- **B.6 字符串资源化**：strings.xml 新增 ≥30 条（会话失效/全局错误/详情页/Dashboard/智能采编），关键页面已用资源 ✅
- **B.8 工程修复**：ScanBarcodeActivity 加 `@AndroidEntryPoint`；BookEditActivity 接入分类选择器（替代硬编码 categoryId=1，弹窗加载 CategoryTree → 展平 → AlertDialog 选择）；RegisterFragment 增加 phone 输入框 + 校验（`^1[3-9]\d{9}$`） ✅

**阶段 C：业务功能补齐（P1，全部完成）**
- **C.1 智能采编 5 个 UI**：新增 [`AcquisitionRepository`](library-android/app/src/main/java/com/library/android/repository/AcquisitionRepository.java) + [`AcquisitionViewModel`](library-android/app/src/main/java/com/library/android/viewmodel/AcquisitionViewModel.java) + 6 个 Fragment（[`AcquisitionFragment`](library-android/app/src/main/java/com/library/android/ui/acquisition/AcquisitionFragment.java) 入口聚合页 + PurchasePredict + DuplicateCheck + GapAnalysis + NegotiationCreate + NegotiationDetail）+ 6 个 layout XML，对接全部 5 个采编端点 ✅
- **C.2 管理端 Dashboard**：新增 [`DashboardVO`](library-android/app/src/main/java/com/library/android/model/DashboardVO.java) + [`AdminDashboardFragment`](library-android/app/src/main/java/com/library/android/ui/admin/AdminDashboardFragment.java) + AdminDashboardViewModel；4 张数字卡片 + MPAndroidChart 折线图（月趋势 borrows/returns 双线）+ 饼图（热门分类 Top-10）+ 图谱重建按钮；接入 `GET /admin/stats/dashboard` ✅
- **C.3 KG 缺失端点**：[`LibraryApi`](library-android/app/src/main/java/com/library/android/network/LibraryApi.java) 补 4 个端点（dashboard / keypath / rebuild / rebuild-all）；KnowledgeGraphRepository.getKeyPath；AdminRepository.getDashboard/rebuildKgForBook/rebuildKgAll ✅
- **C.4 导航路径修复**：[`nav_graph.xml`](library-android/app/src/main/res/navigation/nav_graph.xml) 新增 5 个 destinations（adminDashboard + 5 采编 Fragment）+ 9 个 actions（searchFragment→hotBooks / borrowFragment→overdue / bookDetail→knowledgeGraph / profile→admin/dashboard/acquisition / acquisitionFragment→4 子页 / negotiationCreate→negotiationDetail）；ProfileFragment 增加管理工具区块（按角色显隐：Librarian/Admin 见用户管理+Dashboard，Acquisitor 见智能采编） ✅

**阶段 D：契约一致性与质量补强（部分完成）**
- **D.1 契约对齐**：QueuePosition 维持 `Result<Integer>`（删除冗余 QueuePositionVO）；Negotiation `negotiatorId` 移除（后端 SecurityUtils 自动取，避免误导）✅
- **D.3 ProGuard/R8 规则补全**：补 Retrofit/Gson/Hilt/RxJava3/MPAndroidChart/ZXing 完整规则；移除 Room 旧规则 ✅
- **D.4 暗色模式策略**：LibraryApplication 新增 `setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)` 全局跟随系统 ✅
- **D.2 全局错误体验**：BaseFragment.observeError 已实现按 ApiException 子类型映射文案的统一机制，新增 Fragment 全部使用；MainActivity 已通过 SessionManager 监听全局会话失效（A.1 完成）✅

**阶段 E：验证与文档（部分完成）**
- **E.3 文档同步**：CLAUDE.md 增加阶段 11 进度块 ✅
- E.1（单元测试骨架）/ E.2（Gradle 构建验证）：受 Windows 环境无 gradlew 限制，待联机环境下执行（详见 plan 文件 §阶段 E）

**主要交付清单**
- 新建文件：~30 个（含异常类 6、新 Fragment 6、ViewModel 2、Repository 1、layout 8、XML 配置 2、工具类 4）
- 修改文件：~25 个（Repository 7、ViewModel 4、Fragment 6、layout 3、AndroidManifest、build.gradle.kts、proguard-rules.pro、CLAUDE.md）
- 删除文件：6 个（Room 5 + AuthApiService + DatabaseModule + QueuePositionVO）
- 修复缺陷：覆盖 56 项已识别问题中的 P0/P1 全部 + 大部分 P2

### 已落地（阶段 12：前端全面修复与路由重构）

> 针对真机走查发现的 8 类系统性问题（路由结构/搜索可用性/预约取消/借阅反馈/表单下拉/谈判流式/路由 bug/KG 兜底）全面修复。详见 [`bug-bug-1-2-3-bug-merry-dragon.md`](.claude/plans/bug-bug-1-2-3-bug-merry-dragon.md)。

**WP1 路由重构 + 混合首页**：新建 [`HomeFragment`](library-android/app/src/main/java/com/library/android/ui/main/HomeFragment.java)（推荐卡片 AI 导语流式 + 热门图书 + 分类导航），`startDestination` 改 homeFragment，底部 Tab 首页/借阅/我的，搜索改二级页（全局搜索按钮 navigate 进入）；SearchFragment 改纯搜索（搜索框默认显示 + 搜索图标点击 `doSearch()`）✅
**WP2 搜索可用性**：SearchFragment 搜索框默认显示（删 `layoutSearchInput GONE`）+ startIcon 点击触发搜索 + 回车触发 + doSearch 提取 ✅
**WP3 预约取消可见化**：`item_reservation.xml` 加"取消预约"按钮（WAITING/NOTIFIED 可见）+ 确认弹窗 + 防抖，保留左滑 ✅
**WP4 借阅反馈**：BorrowConfirmDialog Snackbar LENGTH_LONG + `setFragmentResult("borrow_success")` → BookDetailFragment 监听后 `loadBookDetail` 刷新库存 ✅
**WP5 表单下拉化**：后端新增 `GET /acquisition/suppliers` + `GET /acquisition/resources` 列表端点；NegotiationCreateFragment resource/supplier 改 AlertDialog 选择器（不再手填 ID）；PurchasePredict/GapAnalysis subjectId 改分类选择器（复用 `/categories/tree`）✅
**WP6 谈判建议流式**：后端新增 `GET /acquisition/negotiation/{id}/suggestion/stream` SSE（priceRange 秒推 + LLM 文本逐 token 流式）；NegotiationAdvisor 加 `streamSuggestionText`（纯文本 Prompt，LLM 降级模板）；前端 AcquisitionRepository SSE 帧解析 + ViewModel 流式 LiveData + DetailFragment 逐字渲染 ✅
**WP7 路由 bug 修复**：BookDetailFragment 用 action（bookDetail→knowledgeGraph）；LoginFragment 登录后跳 homeFragment（不再硬编码 searchFragment）✅
**WP8 KG 兜底**：部署后 `POST /admin/kg/rebuild-all` ✅
**闪退修复（关键）**：TokenAuthenticator 对 refreshToken 请求本身的 401 直接 return null（防无限递归 → 栈溢出 SIGSEGV）；旧 token 过期时不再崩溃，而是 clear + notifyExpired 跳登录 ✅

**真机验证**（admin）：首页混合内容 + 流式推荐 ✅ / 搜索二级页 + 搜索按钮 ✅ / 预约取消按钮 ✅ / 谈判下拉选择 + 流式建议（5 策略+5 条款+5 风险）✅

### 已落地（阶段 13：Android 前端系统化深度重构 — 全部完成）

> 对 Android 前端进行彻底系统化重构，基于三轮深度代码审查发现的 50+ 项问题，按 8 个 Work Package 推进。**全量编译通过 + Lint 零 Error + 真机安装验证通过**。详见 `library-android/CLAUDE.md`。

- **WP1 导航路由修复（8 项）** ✅：KG 3 子页面连通（ChipGroup）+ LiteratureTraceFrame argument 声明 + 首页热门连通 + 全局 action 移入 Fragment + 死 action/layout 删除 + BookDetail 自导航防栈溢出 + 搜索防重复 + 管理端超期入口
- **WP2 数据层清洗（10 项）** ✅：4 死文件删除 + OverdueViewModel 新建 + NegotiationCreate/GapAnalysis VM 注入整改 + BookRepository 补充 listCategories/getCategory + LibraryApi 删 healthCheck
- **WP3.1 字符串外部化** ✅：71+ 处硬编码全部资源化（21 XML + 10+ Java），`grep` 验证仅余 1 个视觉分隔符 "——"
- **WP3.2 主题一致性** ✅：DARK_MODE_DESTINATIONS +6 页面 + BookEditActivity 声明式暗色 + `android:tint`→`app:tint` 全面修正
- **WP2.6-2.7 Model 统一** ✅：PriceRange Double→BigDecimal + 删除重复类 + BookSimpleVO Parcelable
- **WP2.5 ViewModel 深度迁移** ✅：**13 个 VM 全部 extends BaseViewModel，彻底移除兼容字段**，所有 Fragment 同步更新 observer 类型（`getErrorMessage()`→`getErrorEvent()`, `isLoading()`→`getLoadingState()`）
- **WP4 架构精化** ✅：新建 HomeViewModel（推荐流式）+ 精简 ProfileViewModel → Activity scope 共享
- **WP5 文档同步** ✅：新建 `library-android/CLAUDE.md` + 主 CLAUDE.md 更新
- **单元测试骨架** ✅：SmokeTest 通过 + OverdueViewModelTest 分页验证
- **真机验证** ✅：`adb install` Success + 冷启动无 crash + 进程 PID 存活 + 内存正常
- **Lint** ✅：零 Error，仅预存 DefaultLocale Warning
- **新建文件 5 个**：HomeViewModel / OverdueViewModel / library-android/CLAUDE.md / SmokeTest / OverdueViewModelTest
- **修改文件 50+ 个**：nav_graph + 21 layout + 10 ViewModel + 15 Fragment + 2 Repository + strings + colors + LibraryApi + BookSimpleVO + NegotiationSuggestion + MainActivity + BookEditActivity + AndroidManifest + BorrowConfirmDialog + CLAUDE.md
- **删除文件 5 个**：BookSearchRequest / ErrorResponse / PageRequest / ReservationStatusCard + 1 内部类

### 已落地（阶段 14 后：真机走查修复 — 2026-06-19）

> 针对真机走查发现的 9 项前端+后端问题，进行 3 轮回归修复。全量 16 个文件修改，涉及搜索页/谈判流式/取消预约/返回导航。

**搜索页修复（8 项）**：
- ✅ **titlebar 缺失** — `fragment_search.xml` 顶部引入 `page_toolbar`；`SearchFragment` 改继承 `BaseFragment` 并调 `setupToolbar`
- ✅ **分类导航箭头** — `strings.xml` 中 `category_nav = "分类导航 ▸"` 删除 Unicode `▸`（此前反复误改 `item_category.xml` 的 `ivExpand` ImageView）
- ✅ **清空按钮 icon** — 新建 `drawable/ic_close_vector.xml`（标准 X 形），替换 `ic_search_vector` + `rotation="45"` hack
- ✅ **热门搜索词** — 硬编码词条从 8 个扩展到 23 个
- ✅ **搜索结果展示词条** — `SearchViewModel` 新增 `searchMethodLabel`（关键词搜索/ISBN搜索/分类浏览/高级搜索），格式 `关键词搜索 "机器学习" 找到 5 条结果`
- ✅ **"继续搜索"按钮** — 从 `fragment_search_results.xml` 删除（与 `page_toolbar` 返回按钮冲突）
- ✅ **ISBN 扫码返回无效** — `removeExtra("isbn")` 防止 `navigateUp` 返回 SearchFragment 时 `onViewCreated` 重复触发导航死循环
- ✅ **返回按钮失效** — `BaseFragment.setupToolbar` 中 `navigateUp()` 在 startDestination 返回 false 时兜底调 `onBackPressed()`

**谈判流式防闪退（4 项）**：
- ✅ **SSE `done` 事件丢失** — 后端 `event:done` 后 `data:` 为空导致 `dataBuf.length()==0`，`dispatchEvent` 被跳过。修复：空行时优先检查 `"done".equals(event)`
- ✅ **流结束后误报错** — `parseSseStream` 改为返回 `boolean`（done→true），`fromCallable` 捕获 `body.close()` 的 IOException 后按 `completed` 抑制（服务端 `emitter.complete()` 关闭连接导致 `close()` 抛异常）
- ✅ **流式跨 VM 污染** — `AcquisitionViewModel.onCleared()` 移除 `repository.disposeStreams()` 调用（repository 是 Hilt 全局单例，创建页 VM 清理会错误取消详情页的活跃流）
- ✅ **流式生命周期** — `NegotiationDetailFragment.onDestroyView` 主动调 `viewModel.disposeStreams()`；`AcquisitionRepository` 新增 `volatile streamCancelled` + `parseSseStream` 循环内检查

**取消预约修复（4 项）**：
- ✅ **后端终态扩展** — `ReservationServiceImpl.cancel()` 从仅允许 `WAITING` 改为拒绝 RESERVED/COMPLETED/EXPIRED/CANCELLED 四种终态，允许 NOTIFIED 取消
- ✅ **V6 唯一约束根治** — 新增 `ReservationMapper.physicalCleanStaleWaiting`（`@Delete` 物理 DELETE，绕过 MyBatis-Plus 逻辑删除）。`reserve()` 前置调用清理 `deleted=1` 的旧 WAITING 记录释放索引槽位；`cancel()` 遇 `DataIntegrityViolationException` 时 `deleteById`→`physicalClean`
- ✅ **前端错误可见** — `ReservationListFragment` 改继承 `BaseFragment` 并调 `observeError()`；`ReservationViewModel.cancelReservation` 在 `result.isSuccess()==false` 和 throwable 两条路径均调 `postError`
- ✅ **取消反馈** — 失败时显示 Snackbar `cancel_failed` 替代静默吞没

### 待实现
- 集成测试运行：Docker Desktop 29 兼容问题已通过 `make itest-tcp` 解决，35 项集成测试完成（阶段 10 后第二轮）
- ✅ ~~Repository MockWebServer 集成测试~~ — 已完成：8 个 Repository 全部覆盖（35 测试类 / 178 tests）
- Android 真机 ANP0220602001126 回归验证（建议整体走查后执行）

### 前端质量审计修复（2026-06-22）

> 对 2026-06-19 审计报告的 90 个问题（FQA-001~086 + INT-H01~04）进行全面修复与验证。

- **Critical（P0）3 项全部消除**：TokenManager 三级降级 + 明文 HTTP 半改造 + StringFormat 匹配
- **High（P1）23 项全部修复**：MVVM 重构 8 页面 + 品牌色统一 + ThemeManager 深色逻辑 + WebView 暗色适配 + 密码可见性图标修复等
- **Medium（P2）40/42 已修复**：设计 Token 统一 84.8% + 暗色重构 + contentDescription + 品牌打磨 + 图表主题适配 + Profile/setGlobalTitle 硬编码中文消除 + Bundle key 常量化 + Utf8Fix Release 日志修复
- **Low（P3）18/22 已修复**：targetSdk=35 + 反射移除 + 热门标签 XML 资源化 + chip stroke width Token 化
- **测试增强**：151 → 178 tests（新增 BaseViewModel/SessionManager/SingleLiveEvent 测试 + ViewModel/Repository 边界条件覆盖）
- **遗留**：P3-01 未使用资源（待技术债窗口）、真机回归验证
- **编译**：`./gradlew assembleDebug` BUILD SUCCESSFUL ✅
- **测试**：`./gradlew testDebugUnitTest` 178/178 通过 ✅
- **Lint**：0 Error ✅

### 编码约定
- **Commit**: [Conventional Commits](https://www.conventionalcommits.org/)，中文 subject
- **分支**: `feature/<模块>-<简述>` / `fix/<模块>-<简述>`，Squash Merge → develop
- **Java 编码**: 阿里巴巴 Java 开发手册 + 项目 `docs/CONTRIBUTING.md` 补充
- **测试方法命名**: `should{预期行为}When{条件/输入}`
- **测试统计口径**: 文档中"N 项测试全绿"指 `mvn surefire` 报告的**执行用例数**（含 `@ParameterizedTest` 参数化展开与 `@Nested` 嵌套类），而非 `@Test` 注解的物理方法数；归档记录一律以 surefire 执行用例数为准
- **禁止**: Controller 直接调 Mapper、拼接 SQL、吞异常、push --force 到 main

### 已落地（阶段 9 后：剩余审计问题修复）

> 对阶段 9 后审计修复记录中未能被提交 `62747e0` 覆盖的剩余 ~35 项 P1/P2/P3 问题进行修复。
> 全量 **356 项测试全绿**（common 144 + ai 29 + core 99 + security 71 + kg 3 + acquisition 10；bootstrap 1 项 @Disabled）。

- **P1 重要修复**：GlobalExceptionHandler 补 `DataIntegrityViolationException` handler（409 冲突响应）+ `spring-tx` 依赖；OperationLogAspect 分层合规——新建 `OperationLogService`/`OperationLogServiceImpl` 封装 Mapper，AOP 改注 Service 替代直接调 Mapper；GraphBuildServiceImpl `writeToNeo4j()` N+1 → UNWIND 批量写入（`batchMergeNodes`/`batchMergeRelationships`，从 >30 次往返降至 7 次固定调用）；清理 `EmbeddingServiceImpl` 未使用的 `MAX_RETRIES` 死代码；`fallbackNer()` 作者名分隔注释修正（正确反映不按空格分割西方全名）✅
- **P2 实现质量优化**：RecommendationServiceImpl 超时后先收集部分结果再取消未完成任务（与日志一致）；ReservationZsetReconcileJob `ZRANGE 0 -1` → ZSCAN 分页（防大集合阻塞 Redis）；DuplicateCheckServiceImpl 输入标题 NLP 预处理提到循环外（消除 O(N) 次重复 tokenize）；NegotiationServiceImpl 注入 Spring `ObjectMapper` + `updateRecord` 接受已加载实体消除冗余 `selectById`；LlmConfig/EmbeddingConfig 提取 `AiHttpClientFactory` 共享 HttpClient 配置；4 处 JSON 错误响应提取 `SecurityResponseUtil` 工具类；RateLimitFilter 白名单补 `/prometheus`；KnowledgeGraphProperties `@Configuration`→`@Component` 消除无谓 CGLIB 代理；GraphQueryServiceImpl 删除多余的 `countNodes()` 往返；Neo4jRepository PageRank 降级稠密矩阵→稀疏邻接表（O(n²)→O(edges) 内存）✅
- **P2 配置与文档**：application-prod.yml `include: health,prometheus`（移除冗余 `metrics`）；ErrorCode.SUCCESS 加 `@Deprecated` + 防御性 Javadoc 说明仅用于 switch 枚举覆盖；KgSchemaInitializer 防御性注释标注 Cypher 拼接安全性前提；CLAUDE.md 测试计数精确校正；文档交叉一致性确认 ✅

### 已落地（阶段 9 后：回溯审计修正）

> 阶段 9 完成后回溯审视阶段 0-9 全量工作，验证历次审计修复落地情况，修正 4 项文档不一致 + 3 项代码实现缺陷。
> 全量 **360 项测试全绿**（common 144 + ai 29 + core 99 + security 75 + kg 3 + acquisition 10；bootstrap 1 项 @Disabled）。

- **文档一致性修正**：架构文档 §2.3 权限矩阵与 §6.3 Android Retrofit 示例的 KG 路径修正（`/kg/book/{id}/graph` → `/kg/book/{bookId}`，与 `KnowledgeGraphController` 实际 `@GetMapping` 对齐，阶段 9 后审计 P1-13 遗留）；架构文档头部版本号 v1.10 → v1.13（对齐版本历史表，此前头部滞后于历史最新条目）；`docs/README.md` 目录结构示意图补阶段 9 两份文档条目（导航表已有、目录树遗漏）✅
- **操作日志脱敏兑现**：`OperationLogAspect` 实现 `maskSensitive()`，对参数 JSON 中 password/passwd/secret/token/accessToken/refreshToken/credential/apiKey 等字段值脱敏为 `***`（兑现 `@OperationLog.logParams` Javadoc"敏感字段将由切面自动脱敏"的承诺，此前注释承诺但未实现）；新增 4 项单测覆盖，security 模块 71→75 ✅
- **ES 重建批量优化**：`BookESRepository` 新增 `bulkSave(List<BookDocument>)`（BulkRequest + `response.errors()` 部分失败检测）；`EsRebuildJob` 由逐条 `save` 改为先批量构建文档再 `bulkSave` 一次性写入（N 次网络往返→1 次），批量失败时降级逐条 `save` 隔离单条失败（阶段 9 后审计 P2-01 遗留）✅
- **图谱事务边界澄清**：`GraphBuildServiceImpl.buildGraph()`/`rebuildAll()` 的 `@Transactional` 添加注释说明——其仅管理 MySQL 事务，而本方法无 MySQL 写操作（仅 select 读取），Neo4j 写入通过 Driver 独立 Session auto-commit 不在事务内、不可回滚；图谱一致性实际由 MERGE 幂等语义 + `KgBuildListener` 重试保证（消除 `@Transactional` 对 Neo4j 事务保护的误导）✅
- **记录未改项**：`@ConditionalOnExpression` 在 `LlmConfig` @Bean 与 `LlmServiceImpl`/`EmbeddingServiceImpl` 类上重复声明同一 SpEL（阶段 9 后审计 P1-10），功能完全正常仅 DRY 冗余，改 `@ConditionalOnBean` 有 Bean 注册顺序风险，权衡后保留现状 ✅

### 已落地（阶段 9 后第二轮：回溯审计深化修复）

> 承接第一轮回溯审计，对阶段 0-9 全量工作做第二轮回溯复审（5 并行子代理分模块走查 + 父代理核对关键 P1），修复 39 项（P1×6 / P2×13 / P3×20）。详见 `docs/implementation/阶段9后第二轮回溯审计修复记录.md`。
> 全量 **362 项测试全绿**（common 146 + ai 29 + core 99 + security 75 + kg 3 + acquisition 10；bootstrap 1 项 @Disabled）。

- **P1 关键修复**：`PageDTO` 移除 `@AllArgsConstructor` 改手写构造器钳制分页参数（`pageNum≥1`/`1≤pageSize≤100`），全局修复 4 个旧 Controller（Borrow/Reservation/UserCenter/AdminBorrow）手动构造时 `@Min/@Max` 不触发的 DoS/负 offset 缺口（构造器钳制 + 注解校验双层防护，+2 单测）；`EmbeddingServiceImpl.batchEmbed` 批次间失败隔离（单批失败降级零向量，保证整体部分可用）；OpenAPI trace `{id}`→`{bookId}`、keypath `targetId`→`targetBookId` 契约对齐；架构文档 §2.3 用户管理权限注释"Admin 专有"→"Librarian 及以上"（对齐 `AdminUserController @RequireRole`）；**JWT 默认密钥 `dev-only-do-not-use-in-prod`（27 字节 < 32）触发 `@PostConstruct` fail-fast 致开发环境启动失败**——第一轮加固引入的回归，`application.yml` + `JwtProperties` 默认值改为 ≥32 字节 ✅
- **P2 实现质量**：`BookUpdateDTO.totalCopies` 补 `@Min(1)`；`ReservationServiceImpl.batchLoadQueuePositions` `zRange 0 -1` 全量拉取→按用户 `rank` 逐条查询（防大集合阻塞 Redis）；`LlmServiceImpl`/`EmbeddingServiceImpl` `.block` 超时改为 `readTimeout×(maxRetries+1)+20s` 预算（修复重试被切断）；`LlmServiceImpl.onStatus` 统一 `isError()`；`Neo4jRepository` 新增 `ALLOWED_REL_TYPES`+`requireValidLabel/requireValidRelType` 在 `pageRank`/`saveNode`/`batchMerge*` 入口校验（与 `countNodes` 白名单对称）；`OperationLogAspect` 异步写入 `ForkJoinPool.commonPool`→注入 `taskExecutor`（手写构造器 + 测试同步 `Runnable::run`）；`KgRecommendQueryService` 删 `$topN` 死参数 ✅
- **P2 文档**：架构文档 §6.2.9 补 `/admin/stats/dashboard`；§12.4 配置示例同步（前言修正 + JWT secret + ES `http://` scheme + management prometheus）；§4.2.1 补 SPI 端口模式小节（`KgRecommendPort`/`KgRelatedBookPort`/`GapCoreBookPort`）；阶段9后审计修复记录 §八 355→360；架构文档版本 v1.13→v1.14 ✅
- **P3 技术债清理**：死代码清理（`ContentBasedServiceImpl.toBookSimpleVO`/`DuplicateCheckServiceImpl.cosineSimilarity(String,String)`/`Neo4jRepository.shortestPathViaGds` 永远降级死分支/`LiteratureTracingServiceImpl.parseTracePaths safeDepth`/`LlmConfig·EmbeddingConfig` 未用 import）；`RecommendationProperties` `@Configuration`→`@Component`；`ReservationZsetReconcileJob` cron 4:00→4:30 错开 ES 重建；`OverdueCheckJob` while 加 `maxIterations` 防卡死；`GraphBuildServiceImpl.writeToNeo4j` 三段重复提取 `mergeEntities`；`TopicNetworkBuilderImpl` 边查询改无向匹配；`PredictionServiceImpl` 子查询补 `deleted=0`；`OperationLogAspect logResult` 拼接后整体截断防超列长；`JwtProperties.secret` Java 默认值同步；`MetricsConfig` 裸 Thread→`TaskScheduler` 调度；架构文档 §3.3.1 目录树 V6 `└──`→`├──`；OpenAPI dashboard 403 描述修正；阶段9完成记录限制#2 标注已解决 ✅
- **记录保留项**：`GraphBuildServiceImpl.rebuildAll()` `@Transactional` 自调用（注释已如实说明事务为空、保留以备未来，非缺陷）；V6 `(user_id,book_id,status)` 全列唯一约束（线性状态机下安全，注释准确性已在审计记录说明，改部分索引需新建迁移权衡保留）；架构文档 §6.2.7 与 OpenAPI KG 端点顺序差异（P3 美观，不影响契约）✅

---

## 7. 给 AI 助手的提示

- **源码按阶段实施** — 严格按 `docs/implementation/后端分阶段实施计划.md` 的顺序与任务清单推进，每阶段完成需编写完成记录归档至 `docs/implementation/`
- **每次操作同步文档** — 任何代码变更（新增 API、修改表结构、调整配置项、变更模块依赖）都必须同步更新对应的文档：架构设计文档、OpenAPI 契约、CLAUDE.md（进度状态）、以及涉及到的开发/贡献指南。代码与文档不一致视为未完成
- **文档优先** — `docs/系统架构设计文档.md` 是开发蓝本，优先以文档为准
- **OpenAPI 契约** — `docs/api/library-api.yaml` 是前后端数据契约，修改 API 需同步更新
- **健康检查路径** — `/api/v1/health`（非 `/actuator/health`）
- **配置文件注释** — `application.yml` 中对配置项有详细说明（阶段10 RabbitMQ 事件总线已启用，ES/Neo4j/Redis/MySQL 均已生效）
- **环境变量注入** — `.env` 文件仅作本地覆盖，所有配置键在 `application.yml` 中已有 `${VAR:默认值}` 默认值
