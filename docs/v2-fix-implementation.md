# V2 评估报告 P0/P1 修复实施记录

> 实施日期：2026-06-22  
> 关联评估：[backend-quality-assessment-v2.md](backend-quality-assessment-v2.md)  
> 多 Agent 协作：4 个 Agent 按文件互斥原则分工并行执行  
> 测试结果：library-common/ai/core/security/knowledge-graph/acquisition 6 模块 **BUILD SUCCESS**

---

## 一、修复任务清单与 Agent 分工

| Task ID | 修复内容 | Agent | 独占文件 | 状态 |
|---|---|---|---|---|
| P0-1 | KG 增加 CITES 引用边构建 | Agent-KG | `library-knowledge-graph/**` | ✅ |
| P0-2 | 新增 `POST /admin/kg/build-topic-network` + rebuildAll 自动触发 | Agent-KG | 同上 | ✅ |
| P0-3 | JWT 默认密钥占位 + 启动告警 | Agent-Security | `JwtProperties.java`、`application.yml` 之 jwt.secret 段 | ✅ |
| P1-1 | Reservation.setIfAbsent null/false 路径分离 | Agent-Core | `ReservationServiceImpl.java`、`ReservationServiceTest.java` | ✅ |
| P1-2 | acquisition 配置漂移同步 | Agent-Acquisition | `application.yml` 之 acquisition.* 段 | ✅ |
| 附加 | 历史 bug 修复：cancel NOTIFIED 测试与实现一致 | 主控 Agent | `ReservationServiceTest.java` | ✅ |

**资源竞争防控**：
- Agent-KG 与 Agent-Core 完全独立可并行启动
- Agent-Security 与 Agent-Acquisition 都需改 `application.yml`，按"Security 先改 jwt 段 → Acquisition 后改 acquisition 段"串行执行，且明确约束彼此勿动对方字段
- 全程无文件读写冲突

---

## 二、P0-1：CITES 引用边构建

### 文件修改
- [GraphBuildServiceImpl.java](../library-server/library-knowledge-graph/src/main/java/com/library/kg/service/impl/GraphBuildServiceImpl.java)

### 核心改动
```java
// writeToNeo4j() 末尾追加
buildCitationsBySharedKeywords(book, entities);

private void buildCitationsBySharedKeywords(Book book, BookEntityList entities) {
    int totalKeywords = (entities.getKeywords() == null) ? 0 : entities.getKeywords().size();
    if (totalKeywords == 0) return;
    try {
        String cypher = """
                MATCH (newBook:Book {id: $bookId})-[:HAS_KEYWORD]->(k:Keyword)
                      <-[:HAS_KEYWORD]-(oldBook:Book)
                WHERE oldBook.id <> $bookId
                  AND (oldBook.createTime IS NULL OR oldBook.createTime <= $createTime)
                WITH oldBook, count(DISTINCT k) AS shared
                WHERE shared >= 2
                ORDER BY shared DESC LIMIT 5
                MATCH (src:Book {id: $bookId})
                MERGE (src)-[r:CITES]->(oldBook)
                SET r.weight = toFloat(shared) / $totalKeywords
                RETURN count(r) AS created
                """;
        // ... 参数构造与日志
    } catch (Exception e) {
        log.warn("CITES 引用边构建失败 bookId={}: {}", book.getId(), e.getMessage());
    }
}
```

### 业务正当性
课设场景下没有真实的参考文献元数据（CrossRef DOI 引用列表），采用领域内常见的代理方式——同主题图书之间存在隐含的知识传承关系（类似 co-citation analysis）：
- 共享关键词≥2 → 视为存在引用关系
- 方向："新书 → 旧书"（按 createTime 比较）
- 权重：`shared / totalKeywords`，保证 > 0
- 每本图书上限 5 条 CITES 边，防止图谱过密
- MERGE 语义保证幂等

### 新增测试
[GraphBuildServiceImplTest.java](../library-server/library-knowledge-graph/src/test/java/com/library/kg/service/impl/GraphBuildServiceImplTest.java) 3 个用例：
- 有关键词时验证 query 参数含 CITES/MERGE/HAS_KEYWORD
- 无关键词时跳过
- CITES 异常不影响主流程

---

## 三、P0-2：主题网络端点 + rebuildAll 自动触发

### 文件修改
- [KnowledgeGraphController.java](../library-server/library-knowledge-graph/src/main/java/com/library/kg/controller/KnowledgeGraphController.java) 新增端点
- [GraphBuildServiceImpl.java](../library-server/library-knowledge-graph/src/main/java/com/library/kg/service/impl/GraphBuildServiceImpl.java) 注入 `ObjectProvider<TopicNetworkBuilder>`，rebuildAll 末尾自动触发

### 端点定义
```java
@PostMapping("/admin/kg/build-topic-network")
@RequirePermission("kg:admin")
@Operation(summary = "构建主题关联网络", description = "计算关键词 Jaccard 共现 + PageRank 中心度")
public Result<Void> buildTopicNetwork() {
    topicNetworkBuilder.buildTopicNetwork();
    return Result.success();
}
```

### 自动触发逻辑
```java
public int rebuildAll() {
    // ... 分页扫描所有图书并 buildGraph
    log.info("全量重建完成: 成功 {} 本", built);
    // 全量重建后自动触发主题网络（RELATED_TO + PageRank）重建
    try {
        TopicNetworkBuilder builder = topicNetworkBuilderProvider.getIfAvailable();
        if (builder != null) builder.buildTopicNetwork();
        else log.warn("TopicNetworkBuilder 不可用，跳过主题网络重建");
    } catch (Exception e) {
        log.warn("主题网络重建失败（不影响全量重建结果）: {}", e.getMessage());
    }
    return built;
}
```

### 循环依赖规避
`TopicNetworkBuilder` 通过 `ObjectProvider` 延迟解析，即使未来引入反向依赖也不会产生循环依赖。

### 新增测试
[KnowledgeGraphControllerTest.java](../library-server/library-knowledge-graph/src/test/java/com/library/kg/controller/KnowledgeGraphControllerTest.java)：验证 `/admin/kg/build-topic-network` 委托至 `TopicNetworkBuilder.buildTopicNetwork()`

---

## 四、P0-3：JWT 默认密钥占位与启动告警

### 文件修改
- [JwtProperties.java](../library-server/library-security/src/main/java/com/library/security/config/JwtProperties.java)
- [application.yml](../library-server/library-bootstrap/src/main/resources/application.yml) 第 jwt.secret 行

### 修改前
```java
private String secret = "library-system-dev-jwt-secret-replace-in-production-2026";
```
```yaml
secret: ${JWT_SECRET:library-system-dev-jwt-secret-replace-in-production-2026}
```

### 修改后
```java
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /** 默认值仅为开发期占位符（长度 ≥32 满足 @PostConstruct 校验），启动时会发出 warn 告警；
     *  生产环境必须由 JWT_SECRET 环境变量注入强随机密钥 */
    private String secret = "CHANGE_ME_IN_PRODUCTION_AT_LEAST_32_BYTES_LONG";

    @PostConstruct
    void validate() {
        int len = secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length;
        if (len < 32) {
            throw new IllegalStateException(
                    "jwt.secret 必须 >= 32 字节（HS256 要求），当前 " + len
                            + " 字节。请设置环境变量 JWT_SECRET，生成命令：openssl rand -base64 48");
        }
        if (secret.contains("CHANGE_ME") || secret.startsWith("library-system-dev-")) {
            log.warn("⚠ 警告：当前使用开发环境默认 JWT 密钥！生产环境必须通过 JWT_SECRET 环境变量注入强随机密钥，否则任何人均可伪造令牌。");
        }
    }
}
```
```yaml
# 生产必须通过环境变量 JWT_SECRET 注入；占位符长度 ≥32 字节满足启动校验，但仅供开发；启动时会发出警告日志
secret: ${JWT_SECRET:CHANGE_ME_IN_PRODUCTION_AT_LEAST_32_BYTES_LONG}
```

### Profile 覆盖检查
- `application-prod.yml` 第 33–35 行：`secret: ${JWT_SECRET}`（无默认值，强制环境变量注入）→ **保留不动**
- `application-dev.yml` / `application-test.yml`：无 jwt 配置覆盖 → 无需修改

---

## 五、P1-1：Reservation null/false 路径分离

### 文件修改
- [ReservationServiceImpl.java](../library-server/library-core/src/main/java/com/library/core/service/impl/ReservationServiceImpl.java) `reserve()` 方法 L85-93
- [ReservationServiceTest.java](../library-server/library-core/src/test/java/com/library/core/service/ReservationServiceTest.java)

### 修改前
```java
if (!Boolean.TRUE.equals(locked)) {
    throw new BizException(ErrorCode.ALREADY_RESERVED);
}
```

### 修改后
```java
if (locked == null) {
    // Redis 不可用——基础设施问题，不应误报为业务冲突
    log.warn("预约锁获取失败：Redis 不可用，userId={}, bookId={}", userId, bookId);
    throw new BizException(ErrorCode.INTERNAL_ERROR, "系统繁忙，请稍后再试");
}
if (!locked) {
    // 业务冲突：同用户已有进行中的预约或正在并发请求
    throw new BizException(ErrorCode.ALREADY_RESERVED);
}
```

### 新增测试
- `reserve_whenRedisReturnsNull_shouldThrowInternalError`：mock setIfAbsent 返回 null，断言 INTERNAL_ERROR
- `reserve_whenLockAlreadyHeld_shouldThrowAlreadyReserved`：mock setIfAbsent 返回 false，断言 ALREADY_RESERVED

---

## 六、P1-2：acquisition 配置漂移同步

### 文件修改
- [application.yml](../library-server/library-bootstrap/src/main/resources/application.yml) 第 215、218 行

### 修改详情
| 字段 | yml 原值 | yml 新值 | Java 默认 | 同步说明 |
|---|---|---|---|---|
| `acquisition.duplicate-title-threshold` | `0.8` | `0.6` | `0.6` | 注释"WP-0 已放宽" |
| `acquisition.gap-heat-threshold` | `0.7` | `0.4` | `0.4` | 同上 |

两行末尾追加注释：`# 同步自 Java 默认值（WP-0 修复后阈值）`。

---

## 七、附加修复：cancel NOTIFIED 测试与实现一致

### 背景
综合测试时发现 `ReservationServiceTest.Cancel.shouldThrowConflictWhenNotWaiting` 失败——但这是 develop 分支预先存在的历史 bug，与本次 P1-1 修改无关（git stash 状态下仍失败）。

### 业务判断
- `ReservationServiceImpl.cancel()` 注释明确："WAITING/NOTIFIED 允许，已锁定/完成/过期/已取消暂不可逆"
- NOTIFIED 表示"已被通知，48h 内有效"，允许用户主动放弃是合理的
- 实现正确，测试错误

### 修复
[ReservationServiceTest.java](../library-server/library-core/src/test/java/com/library/core/service/ReservationServiceTest.java) Cancel 嵌套类：
- 原 `shouldThrowConflictWhenNotWaiting`（NOTIFIED→CONFLICT）→ 改名 `shouldAllowCancelWhenNotified`，验证 NOTIFIED 可取消
- 新增 `shouldThrowConflictWhenTerminalState`（COMPLETED→CONFLICT）覆盖原意图

---

## 八、综合检查与测试结果

### GetDiagnostics
全项目（含修改文件 + 新建测试）`GetDiagnostics` 返回空，无编译/类型错误。

### Maven 测试
```bash
cd library-server
.\mvnw -o -pl library-common,library-ai,library-core,library-security,library-knowledge-graph,library-acquisition test
```

**结果摘要**（部分模块）：
```
library-common: 全部通过
library-ai: 全部通过
library-core: 全部通过（114 用例，含修复后的 9 个 Reservation 用例 + 2 个新增边界用例）
library-security: 75 个全部通过
library-knowledge-graph: 7 个全部通过
  - GraphBuildServiceImplTest.BuildCitations: 3 用例通过
  - KnowledgeGraphControllerTest: 1 用例通过
  - GdsAvailabilityProviderTest: 3 用例通过
library-acquisition: 15 个全部通过

[INFO] BUILD SUCCESS
```

### 协同正确性检查
1. ✅ Agent-KG 修改的 `GraphBuildServiceImpl.java` 与 `KnowledgeGraphController.java` 通过 `TopicNetworkBuilder`/`ObjectProvider` 正确协同
2. ✅ Agent-Security 修改的 `JwtProperties.java` 与 `application.yml` 中 `jwt.secret` 占位符匹配；prod profile 保持强制环境变量注入
3. ✅ Agent-Acquisition 修改的 `application.yml` 中 acquisition.* 与 `AcquisitionProperties.java` 默认值完全一致；未触碰 jwt 段
4. ✅ Agent-Core 修改的 `ReservationServiceImpl.java` 与 `ReservationServiceTest.java` 用例对齐，且未破坏其他 Reservation 测试

---

## 九、未修复项（后续工作）

| # | 任务 | 优先级 | 阻塞原因 |
|---|---|---|---|
| P1-7 | 启用预约通知端到端集成测试 | 中 | 需升级 Redisson（突破课设范围，建议答辩后处理） |
| P1-8 | README 补充"首次使用需重建图谱"说明 | 低 | 文档类工作，不影响代码功能 |
| P2-* | 账户锁定、KG 单元测试增补、限流 429 测试等 | 低 | 加分项，可视答辩准备时间安排 |

---

## 九.B、遗留 Bug 第二轮修复（2026-06-22 补充）

> 在第二轮综合检查时，发现 v1 评估报告中提到的若干遗留 Bug 在 P0/P1 修复中未涉及，本节追加修复 3 项关键缺陷。剩余 2 项标注为低优先级延后。

### 第二轮分工

| Agent | 独占范围 | 任务 |
|---|---|---|
| Agent-Schedule | `OverdueCheckJob.java` + `OverdueCheckJobTest.java` | B1 |
| Agent-KG-Fix | `Neo4jRepository.java` + `GraphBuildServiceImpl.java` + 删除 `dto/RelationList.java` | B2 + B3 |

并行执行无冲突。

### B1：OverdueCheckJob 收敛 bug

**问题**：当 `processBatch` 因 `DuplicateKeyException` 等导致同批所有条目都跳过（状态仍为 BORROWED/RENEWED）时，下一轮 SQL 会查到完全相同的批次，依赖 `maxIterations=1000` 兜底退出，浪费 DB 资源。

**修复**：引入游标 ID 推进，确保每轮严格扫描更大 ID 的记录。
```java
long lastId = 0L;
while (iteration++ < maxIterations) {
    final long cursor = lastId;
    List<BorrowRecord> batch = borrowRecordMapper.selectList(
        new LambdaQueryWrapper<BorrowRecord>()
            .gt(BorrowRecord::getId, cursor)
            .lt(BorrowRecord::getDueDate, today)
            .in(BorrowRecord::getStatus, BorrowStatusEnum.BORROWED, BorrowStatusEnum.RENEWED)
            .orderByAsc(BorrowRecord::getId)
            .last("LIMIT " + BATCH_SIZE));
    if (batch.isEmpty()) break;
    processedCount += batchProcessor.processBatch(batch, today);
    lastId = batch.get(batch.size() - 1).getId();
}
```

**新增测试**：`shouldAdvanceCursorEvenWhenBatchProcessingFails`——模拟 processBatch 全部返回 0，验证 selectList 仅被调用 3 次（而非 1000 次），证明游标推进让循环正常收敛。

### B2：消除 KG 死代码

**问题**：`extractRelations()`/`fallbackRe()` 返回的 `List<Relation>` 在 `writeToNeo4j()` 中**完全未被消费**——后者只调用 `mergeEntities()` 写硬编码的三类关系，relations 列表被丢弃。

**修复（方案 A：删除死代码）**：
- 删除 `extractRelations(book, entities)`
- 删除 `fallbackRe(book, entities)`
- 删除 `buildRePrompt(book, entities)`
- 删除 `entitiesListStr(...)`
- 删除 `RelationList.java` DTO（Grep 确认无其他引用）
- `writeToNeo4j` 签名从 `(Book, BookEntityList, List<Relation>)` 改为 `(Book, BookEntityList)`
- 类 Javadoc `NER → RE → MERGE` 更新为 `NER → MERGE`

**保留**：`buildCitationsBySharedKeywords` 方法、`ObjectProvider<TopicNetworkBuilder>` 注入、`rebuildAll` 末尾 `buildTopicNetwork` 触发——这些是 P0 修复成果。

### B3：Neo4jRepository.batchMergeNodes key 参数一致性

**问题**：方法签名声称支持任意 `key`，但 row 属性名硬编码为 `"name"`，Cypher 拼接为 `row.name`。传 `key="id"` 时会生成 `MERGE (n:Label {id: row.name})`，row 中无 `id` 属性，导致用 null 创建空节点。当前所有调用方都传 `key="name"` 恰好掩盖了 bug。

**修复**：
```java
public void batchMergeNodes(String label, String key, List<String> keyValues) {
    requireValidLabel(label);
    requireValidIdentifier(key);  // 新增白名单校验
    if (keyValues == null || keyValues.isEmpty()) return;
    final String keyParam = key;
    Map<String, Object> params = Map.of("rows",
            keyValues.stream().map(v -> Map.<String, Object>of(keyParam, v)).toList());
    String cypher = "UNWIND $rows AS row MERGE (n:" + label + " {" + key + ": row." + key + "})";
    execute(cypher, params);
}
```

新增 `IDENTIFIER_PATTERN = "[A-Za-z_][A-Za-z0-9_]*"` 与 `requireValidIdentifier()` 校验，防止任意字符串拼接 Cypher。其余方法（saveNode/saveRelationship/batchMergeRelationships）未触碰。

### 测试结果

```
.\mvnw -o -pl library-common,library-ai,library-core,library-security,library-knowledge-graph,library-acquisition test
[INFO] BUILD SUCCESS
```
所有模块全部通过，含新增的 OverdueCheckJob 游标测试。

### 第二轮未修复项（延后）

| Bug | 现状 | 不修复理由 |
|---|---|---|
| `LlmServiceImpl.chatStream` onErrorResume 静默吞错 | P2 | 课设场景下 LLM 失败概率低，前端 SSE done 事件可触发降级文案 |
| `returnBook` 乐观锁只重试一次 | P2 | 课设单实例 + 低并发，触发概率极低 |

---

## 九.C、生产部署执行记录（2026-06-22 12:14-12:16）

> 所有 P0/P1 + 第二轮 B1/B2/B3 修复已部署至生产服务器并通过验证。

### 部署目标
- 服务器：`101.132.24.73`（前端对接 IP）
- 部署目录：`/opt/library/`
- 启动方式：`nohup java -jar app.jar --spring.profiles.active=prod ...`（非 systemd）

### 部署流程
1. **本地打包**：`.\mvnw clean package -DskipTests -pl library-bootstrap -am` → 产物 `library-bootstrap-0.0.1-SNAPSHOT.jar` (105.05 MB)
2. **上传 jar**：`scp` 上传至 `/opt/library/app.jar.new`（MD5 校验：7ddb5d10... 与本地一致）
3. **备份现有 jar**：`cp -p app.jar app.jar.bak.20260622_121432`（保留最新 4 个备份）
4. **优雅停旧进程**：`kill 1933171` → 2 秒后退出（无需 SIGKILL）
5. **原子替换**：`mv app.jar.new app.jar`
6. **启动新进程**：加载 `.env.prod` 后台启动 → PID 849261，日志 `logs/app.20260622.log`
7. **健康检查**：57s 后 `/api/v1/health` 返回 `{"status":"UP"}`

### 部署验证结果
| 项 | 结果 | 解读 |
|---|---|---|
| `/api/v1/health` | `{"status":"UP"}` | 应用正常 |
| **POST `/admin/kg/build-topic-network`** | **401** | ✅ 新端点存在并启用鉴权（401 ≠ 404=不存在） |
| POST `/admin/kg/rebuild-all` | 401 | 对照（旧端点同样行为） |
| 启动耗时 | 57.343s | 正常 |
| 端口 8080 | 监听中 | 服务可用 |
| Neo4j GDS | available=true, 409 procedures | 主题网络 PageRank 走 GDS 路径 |
| ES 索引 `books` | 已存在 | 搜索可用 |
| JWT 警告 | 未出现 | `.env.prod` 已正确注入强随机 `JWT_SECRET` |

### 验证演示路径
答辩前请在生产环境以 ADMIN 角色：
1. `POST /api/v1/admin/kg/rebuild-all` → 重建图谱（含 CITES 边）
2. `POST /api/v1/admin/kg/build-topic-network` → 触发 PageRank（也可省略，因为 rebuildAll 末尾会自动调用）
3. 前端打开三大功能验证非空：
   - `/kg/search?entity=Java` → 实体列表
   - `/kg/book/{bookId}/trace?direction=BOTH&maxDepth=3` → CITES 路径
   - `/kg/subject/计算机科学?topK=20` → 关键词网络

## 九.D、生产环境真机验证发现的 3 个补充缺陷修复（2026-06-22 12:25-13:00）

> 部署到生产后用 admin 实测三大功能，发现日志中 CITES 创建数恒为 0、学科网络节点重复——本节追加 3 项修复。

### 缺陷与修复

| # | 缺陷 | 根因 | 修复 |
|---|---|---|---|
| **C1** | CITES 边构建始终 0 条（导致文献溯源空白） | `buildCitationsBySharedKeywords` 的 Cypher 在 Neo4j 5.x 中存在语法错误 `Invalid input 'ORDER': expected ...`——WHERE 后裸接 ORDER BY/LIMIT 不被允许；且 Book 节点未持久化 `createTime` 属性，相关过滤条件无效 | 改写 Cypher：① 移除 `createTime` 条件，改用 `oldBook.id < $bookId` 作方向代理（较小 id 通常更早入库）；② 用第二个 `WITH oldBook, shared` 包装 `ORDER BY shared DESC LIMIT 5`，绕开 Cypher 5.x 语法限制 |
| **C2** | 学科网络节点重复（前端展示同名关键词出现 N 次） | `buildSubjectNetwork` 的 MATCH 路径 `(s)<-[:BELONGS_TO]-(:Book)-[:HAS_KEYWORD]->(k)` 在多本图书共享同一关键词时产生 N 条路径，每条返回一行 | `RETURN id(k)` 前加 `DISTINCT` 关键字 |
| **C3** | 生产数据量过少（22 本书集中在 13/39 分类） | V100 测试种子未在生产补充扩展 | 编写 `seed-extra-books.sql` 增量补 41 本，覆盖 33/39 分类（85%） |

### 修复代码位置
- [GraphBuildServiceImpl.java#L287-L320](../library-server/library-knowledge-graph/src/main/java/com/library/kg/service/impl/GraphBuildServiceImpl.java)（CITES Cypher）
- [TopicNetworkBuilderImpl.java#L77-L92](../library-server/library-knowledge-graph/src/main/java/com/library/kg/service/impl/TopicNetworkBuilderImpl.java)（buildSubjectNetwork DISTINCT）
- [seed-extra-books.sql](../seed-extra-books.sql)（生产种子扩展）

### 验证结果（生产真机数据）

| 指标 | 修复前 | 修复后 |
|---|---|---|
| Book 节点 | 21 | **62** |
| CITES 引用边 | **0** | **52** ✅ |
| RELATED_TO 边 | 4861 | 3176 |
| 带 PageRank 的 Keyword | 380 | 505 |
| 分类有书覆盖率 | 13/39 (33%) | **33/39 (85%)** |

### 三大功能真机演示数据

**文献溯源** `/kg/book/10012/trace`（神经网络与深度学习）→ **105 条路径**，示例：
```
神经网络与深度学习 → 统计学习方法 → 机器学习 → 深度学习
```

**实体搜索** `/kg/search?entity=Java` → 7 个混合节点（KEYWORD/BOOK/SUBJECT）

**学科网络** `/kg/subject/计算机科学?topK=15` → **15 个去重节点 + 15 条边**：
```
编程(2.79) → 基础 → 机器学习 → 深度学习 → 教程 → 网络安全 → Java → 数据库...
```

**学科网络** `/kg/subject/中国古典文学` → 10 个节点：
```
小说(2.39) → 四大名著 → 古典小说 → 红楼梦 → 林黛玉 → 贾宝玉 → 章回体 → 石头记
```

### 生产部署流程（含数据迁移）
```bash
# 1. 本地打包修复版
cd library-server && .\mvnw clean package -DskipTests -pl library-bootstrap -am

# 2. 上传 jar 与种子
scp library-server/library-bootstrap/target/library-bootstrap-0.0.1-SNAPSHOT.jar root@101.132.24.73:/opt/library/app.jar.new
scp seed-extra-books.sql root@101.132.24.73:/opt/library/

# 3. 重启服务（在服务器）
cd /opt/library && cp app.jar app.jar.bak.$(date +%Y%m%d_%H%M%S)
pkill -f /opt/library/app.jar && sleep 5
mv app.jar.new app.jar
nohup java -jar app.jar --spring.profiles.active=prod >> logs/app.$(date +%Y%m%d).log 2>&1 &

# 4. 导入扩展数据
. /opt/library/.env.prod
docker exec -i library-mysql-prod mysql -uroot -p${MYSQL_ROOT_PASSWORD} -D library_db < seed-extra-books.sql

# 5. 清空 Neo4j 并触发全量重建（admin 登录后调）
docker exec library-neo4j-prod cypher-shell -u ${NEO4J_USERNAME} -p ${NEO4J_PASSWORD} \
    "MATCH (n) DETACH DELETE n; CREATE CONSTRAINT IF NOT EXISTS FOR (b:Book) REQUIRE b.id IS UNIQUE;"

TOKEN=$(curl -sS -X POST http://127.0.0.1:8080/api/v1/auth/login \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"Admin@2026"}' \
    | grep -oP '"accessToken":"[^"]+' | sed 's/.*"//')

curl -X POST http://127.0.0.1:8080/api/v1/admin/kg/rebuild-all \
    -H "Authorization: Bearer $TOKEN" -m 900
```

### admin 账号说明
- 用户名：`admin`
- 密码：`Admin@2026`（已重置；BCrypt $2b$12 哈希）
- 角色：`ADMIN`

### 回滚预案
若发现新版本有问题：
```bash
ssh root@101.132.24.73
cd /opt/library
pkill -f /opt/library/app.jar
cp app.jar.bak.<最新时间戳> app.jar
nohup java -jar app.jar --spring.profiles.active=prod >> logs/rollback.log 2>&1 &
```

---

## 十、答辩演示前清单

1. 确保 Neo4j + RabbitMQ 容器在线（SSH root 已配置）
2. 启动后端后，以 **ADMIN 账号登录**前端 Admin Dashboard
3. 点击"重建知识图谱"按钮（调用 `POST /admin/kg/rebuild-all`）
   - 此操作会同时：① 重建所有图书的图谱节点；② 创建 CITES 引用边；③ 自动触发主题网络构建（RELATED_TO + PageRank）
4. 验证三大功能：
   - `/kg/search?entity=Java` → 应返回实体列表
   - `/kg/book/{bookId}/trace?direction=BOTH&maxDepth=3` → 应返回 CITES 路径
   - `/kg/subject/计算机科学?topK=20` → 应返回关键词网络（按 PageRank 排序）
5. 启动日志中可观察 JWT 警告（若未设置 `JWT_SECRET` 环境变量）

---

## 附录：修改文件清单（共 8 个文件 + 2 个新建测试）

### 主代码（6 个）
- `library-server/library-knowledge-graph/src/main/java/com/library/kg/service/impl/GraphBuildServiceImpl.java`
- `library-server/library-knowledge-graph/src/main/java/com/library/kg/controller/KnowledgeGraphController.java`
- `library-server/library-security/src/main/java/com/library/security/config/JwtProperties.java`
- `library-server/library-core/src/main/java/com/library/core/service/impl/ReservationServiceImpl.java`
- `library-server/library-bootstrap/src/main/resources/application.yml`（jwt.secret + acquisition.* 三行）
- `library-server/library-core/src/test/java/com/library/core/service/ReservationServiceTest.java`

### 新建测试（2 个）
- `library-server/library-knowledge-graph/src/test/java/com/library/kg/service/impl/GraphBuildServiceImplTest.java`
- `library-server/library-knowledge-graph/src/test/java/com/library/kg/controller/KnowledgeGraphControllerTest.java`

### 文档（2 个）
- `docs/backend-quality-assessment-v2.md`（更新 §3.4 + §六 状态）
- `docs/v2-fix-implementation.md`（本文档，新建）