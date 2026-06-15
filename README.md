<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.0-6DB33F?style=flat-square&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 17">
  <img src="https://img.shields.io/badge/Maven-3.9+-C71A36?style=flat-square&logo=apachemaven&logoColor=white" alt="Maven">
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-7.2-DC382D?style=flat-square&logo=redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/Elasticsearch-8.11-005571?style=flat-square&logo=elasticsearch&logoColor=white" alt="ES">
  <img src="https://img.shields.io/badge/Neo4j-5.17-4581C3?style=flat-square&logo=neo4j&logoColor=white" alt="Neo4j">
  <img src="https://img.shields.io/badge/RabbitMQ-3.12-FF6600?style=flat-square&logo=rabbitmq&logoColor=white" alt="RabbitMQ">
  <img src="https://img.shields.io/badge/license-MIT-green?style=flat-square" alt="License">
</p>

<h1 align="center">📚 LibrarySystem-SIT</h1>

<p align="center">
  <strong>高校图书馆智能管理系统</strong><br>
  基础图书管理 &nbsp;·&nbsp; 学科知识图谱 &nbsp;·&nbsp; 智能采编
</p>

<p align="center">
  <a href="docs/系统架构设计文档.md">📖 架构设计</a> ·
  <a href="docs/DEVELOPMENT.md">🛠 开发指南</a> ·
  <a href="docs/CONTRIBUTING.md">📋 贡献规范</a> ·
  <a href="docs/api/library-api.yaml">🔌 API 契约</a> ·
  <a href="docs/implementation/后端分阶段实施计划.md">🗺 实施计划</a>
</p>

---

## ✨ 功能亮点

<table>
<tr>
<td width="50%">

### 🔍 智能检索
- 全文搜索（IK 分词 + BM25 排序）
- 语义检索 + 搜索自动补全
- 高级组合筛选（作者/ISBN/分类/年份）
- 热点词 Redis 缓存（TTL 30min）

### 📖 借阅管理
- 借书 / 还书 / 续借 全生命周期
- 状态机驱动 + 乐观锁防并发超卖
- 角色借阅上限自动校验
- 超期自动检测 + 罚款计算

### 📋 预约排队
- Redis ZSET 按时间戳公平排队
- 归还自动通知队首读者（48h 确认窗口）
- 超时自动顺延下一位

</td>
<td width="50%">

### 🧠 知识图谱
- Neo4j 图数据库存储主题关联网络
- DeepSeek LLM 实体识别（NER）+ 关系抽取（RE）
- 文献多跳溯源（前向/后向/双向 BFS）
- PageRank 中心度 + Jaccard 共现分析

### 📊 智能采编
- ARIMA 时序预测采购需求量（OLS 回归）
- 三重策略查重（ISBN + More Like This + 余弦相似度）
- 馆藏缺口分析（核心书目覆盖率）
- DeepSeek LLM 谈判策略生成

### 🎯 个性化推荐
- 多路召回（CF + Content + KG）
- 加权融合精排 + LLM 理由生成
- 全部 LLM 调用含降级策略

</td>
</tr>
</table>

---

## 🏗️ 技术架构

```
┌──────────────────────────────────────────────────────────┐
│                    Modular Monolith                       │
│                                                          │
│  library-common     library-ai        library-core       │
│  (公共基础设施)     (LLM/Embed/NLP)   (图书/借阅/推荐)     │
│                                                          │
│  library-kg         library-acquisition  library-security │
│  (知识图谱)         (智能采编)           (JWT/RBAC)       │
│                                                          │
│  library-bootstrap (🚀 启动聚合 · 配置 · Flyway 迁移)     │
└──────────────────────────────────────────────────────────┘
         │              │              │              │
    ┌────┴────┐   ┌────┴────┐   ┌────┴────┐   ┌────┴────┐
    │  MySQL  │   │  Redis  │   │   ES    │   │  Neo4j  │
    │  8.0    │   │  7.2    │   │  8.11   │   │  5.17   │
    └─────────┘   └─────────┘   └─────────┘   └─────────┘
```

| 层级 | 技术选型 |
|------|----------|
| **框架** | Spring Boot 3.5 + MyBatis-Plus 3.5 |
| **安全** | Spring Security + JJWT 0.12（Access 2h / Refresh 7d） |
| **搜索** | Elasticsearch 8.11 + IK 分词器 |
| **图库** | Neo4j 5.17 + APOC + GDS（PageRank / Dijkstra） |
| **缓存** | Redis 7.2（缓存 / 分布式锁 / ZSET 排队 / 令牌桶限流） |
| **消息** | RabbitMQ 3.12（异步事件，待引入 Starter） |
| **迁移** | Flyway 9.22（V1 基线 10 表 + V2 种子数据 + V3 索引补充） |
| **文档** | SpringDoc OpenAPI 2.6 |
| **NLP** | HanLP 1.8 portable（分词 / 关键词提取） |
| **LLM** | DeepSeek API（NER / RE / 推荐理由 / 谈判策略） |
| **Embedding** | 阿里云百炼 text-embedding-v3 |
| **预测** | Apache Commons Math 3.6（OLS 回归简化 ARIMA） |
| **前端** | Android Java 17 · Material Design 3 · Hilt · Retrofit · RxJava 3 |

---

## 🚀 快速启动

### 前置条件

- **JDK 17+** · **Maven 3.9+** · **Docker 24+**

### 1. 克隆 & 启动中间件

```bash
git clone https://github.com/1na-ko/LibrarySystem-SIT.git
cd LibrarySystem-SIT

# 一键启动全部中间件（MySQL / Redis / ES / Neo4j / RabbitMQ）
docker-compose up -d

# 首次需安装 IK 分词器（执行后重启 ES）
docker-compose restart elasticsearch
```

### 2. 启动后端

```bash
cd library-server

# 编译
mvn clean compile -DskipTests

# 启动（开发环境）
mvn spring-boot:run -pl library-bootstrap -Dspring-boot.run.profiles=dev
```

### 3. 验证

```bash
# 健康检查
curl http://localhost:8080/api/v1/health
# → {"status":"UP","components":{"mysql":"UP","redis":"UP",...}}

# Swagger UI
open http://localhost:8080/api/v1/swagger-ui.html
```

---

## 📂 项目结构

```
LibrarySystem-SIT/
├── docs/                              # 📄 项目文档
│   ├── README.md                      #   文档索引
│   ├── 系统架构设计文档.md             #   设计蓝本
│   ├── DEVELOPMENT.md                 #   开发环境指南
│   ├── CONTRIBUTING.md                #   编码规范
│   ├── api/library-api.yaml           #   OpenAPI 契约（28 端点）
│   ├── db/init.sql                    #   Docker MySQL 初始化
│   └── implementation/                #   实施计划 & 进度记录
├── library-server/                    # ☕ 后端 Maven 多模块
│   ├── pom.xml                        #   父 POM（版本 & 插件管理）
│   ├── library-common/                #   公共基础设施
│   ├── library-ai/                    #   AI 基础设施
│   ├── library-core/                  #   核心业务
│   ├── library-knowledge-graph/       #   知识图谱
│   ├── library-acquisition/           #   智能采编
│   ├── library-security/              #   安全认证
│   └── library-bootstrap/             #   启动聚合
├── library-android/                   # 📱 Android 前端
├── docker-compose.yml                 # 🐳 中间件编排
├── .editorconfig                      # 📝 代码风格
├── .gitattributes                     # 🔤 行尾统一
└── CLAUDE.md                          # 🤖 AI 开发指引
```

---

## 📖 文档导航

| 文档 | 适合 |
|------|------|
| [系统架构设计文档](docs/系统架构设计文档.md) | 了解系统全貌—模块、数据库、API、算法 |
| [DEVELOPMENT](docs/DEVELOPMENT.md) | 搭建本地开发环境 |
| [CONTRIBUTING](docs/CONTRIBUTING.md) | 学习 Git 工作流 & 编码规范 |
| [API 契约](docs/api/library-api.yaml) | 前后端接口联调 |
| [实施计划](docs/implementation/后端分阶段实施计划.md) | 追踪开发进度 |

---

## 📝 License

MIT © 2026 LibrarySystem-SIT Team
