# 图书馆智能管理系统 — 开发环境搭建指南

> **适用对象**：本项目全体开发人员  
> **目标**：30 分钟内完成本地开发环境搭建并跑通健康检查  
> **最后更新**：2026-06-15

---

## 目录

1. [环境要求总览](#1-环境要求总览)
2. [基础环境安装](#2-基础环境安装)
3. [中间件安装与配置](#3-中间件安装与配置)
4. [后端项目启动](#4-后端项目启动)
5. [Android 项目启动](#5-android-项目启动)
6. [验证清单](#6-验证清单)
7. [常见问题排查](#7-常见问题排查)
8. [项目结构速览](#8-项目结构速览)
9. [日常开发命令](#9-日常开发命令)

---

## 1. 环境要求总览

| 组件 | 版本要求 | 用途 | 必装 |
|------|----------|------|------|
| **JDK** | 17.x (LTS) | Java 编译运行环境 | ✅ |
| **Maven** | 3.9.x | 项目构建与依赖管理 | ✅ |
| **MySQL** | 8.0.x | 关系型数据存储 | ✅ |
| **Redis** | 7.2.x | 缓存 / 分布式锁 / 预约队列 | ✅ |
| **Elasticsearch** | 8.11.x | 全文搜索 / 自动补全 | ✅ |
| **Neo4j** | 5.x Community | 知识图谱存储 | ✅ |
| **RabbitMQ** | 3.12.x | 异步消息 / 事件总线 | ✅ |
| **Git** | 2.40+ | 版本控制 | ✅ |
| **IntelliJ IDEA** | 2024.1+ (Ultimate 或 Community) | 后端 IDE | 推荐 |
| **Android Studio** | Hedgehog (2023.1.1)+ | Android 前端 IDE | ✅ |
| **Postman / Apifox** | 最新版 | API 调试 | 推荐 |
| **Docker Desktop** | 24+ | 容器化中间件（可选） | 推荐 |

---

## 2. 基础环境安装

### 2.1 JDK 17

**Windows（推荐使用包管理器）**：

```powershell
# 方式一：Scoop（推荐）
scoop install openjdk17

# 方式二：手动安装
# 1. 访问 https://adoptium.net/download/ 下载 Temurin JDK 17
# 2. 安装到 C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot\
# 3. 设置环境变量 JAVA_HOME
```

**配置环境变量**（手动安装时）：

```
系统变量 → 新建：
  变量名：JAVA_HOME
  变量值：C:\Program Files\Eclipse Adoptium\jdk-17.0.9.9-hotspot

系统变量 → Path → 新增：
  %JAVA_HOME%\bin
```

**验证**：

```bash
java --version
# 预期输出：openjdk 17.0.x ...
javac --version
# 预期输出：javac 17.0.x
```

### 2.2 Maven 3.9

**Windows（Scoop）**：

```powershell
scoop install maven
```

**手动安装**：

1. 从 https://maven.apache.org/download.cgi 下载 `apache-maven-3.9.x-bin.zip`
2. 解压到 `C:\tools\apache-maven-3.9.x`
3. 设置环境变量 `MAVEN_HOME` 并加入 Path

**配置 Maven 镜像**（加速依赖下载）：

编辑 `~/.m2/settings.xml`（即 `C:\Users\<你的用户名>\.m2\settings.xml`）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings>
    <mirrors>
        <mirror>
            <id>aliyun</id>
            <mirrorOf>central</mirrorOf>
            <name>Aliyun Maven Mirror</name>
            <url>https://maven.aliyun.com/repository/public</url>
        </mirror>
    </mirrors>
    <profiles>
        <profile>
            <id>jdk17</id>
            <activation>
                <activeByDefault>true</activeByDefault>
                <jdk>17</jdk>
            </activation>
            <properties>
                <maven.compiler.source>17</maven.compiler.source>
                <maven.compiler.target>17</maven.compiler.target>
                <maven.compiler.compilerVersion>17</maven.compiler.compilerVersion>
            </properties>
        </profile>
    </profiles>
</settings>
```

**验证**：

```bash
mvn --version
# 预期输出：Apache Maven 3.9.x ... Java version: 17.0.x
```

### 2.3 Git

```powershell
# Scoop
scoop install git

# 或从 https://git-scm.com/download/win 下载安装
```

**基础配置**：

```bash
git config --global user.name "你的姓名"
git config --global user.email "你的邮箱"
git config --global core.autocrlf true     # Windows 下自动转换换行符
git config --global init.defaultBranch main
```

---

## 3. 中间件安装与配置

> **推荐方案**：使用 Docker Desktop + Docker Compose 一键启动所有中间件，避免逐个安装的繁琐。  
> **备选方案**：逐个本地安装（见各小节"手动安装"部分）。

### 3.0 Docker 方式（一键启动，强烈推荐）

#### 安装 Docker Desktop

1. 访问 https://www.docker.com/products/docker-desktop/ 下载 Windows 版
2. 安装完成后重启电脑
3. 启动 Docker Desktop，等待右下角鲸鱼图标变为稳定状态

#### 启动所有中间件

在项目根目录执行：

```bash
cd f:/CodeforJAVA/LibrarySystem-SIT
docker-compose up -d
```

> **注意**：`docker-compose.yml` 已创建于项目根目录，内容与附录 A 一致。如尚未拉取最新代码，可参照附录 A 手动创建。

**`docker-compose.yml`** 内容见 [附录 A](#附录-a-docker-composeyml)。

**验证容器状态**：

```bash
docker-compose ps
# 预期：所有容器 STATUS 列为 Up (healthy)
```

#### 常用 Docker 命令

```bash
docker-compose up -d          # 启动所有服务
docker-compose down            # 停止并删除容器
docker-compose restart mysql   # 重启单个服务
docker-compose logs -f mysql   # 查看某个服务的日志
docker-compose down -v         # ⚠️ 停止并删除所有数据卷（重置数据）
```

---

### 3.1 MySQL 8.0（手动安装）

**下载安装**：

1. 从 https://dev.mysql.com/downloads/installer/ 下载 MySQL Installer
2. 选择 "Developer Default" 安装类型
3. 设置 root 密码为 `root123456`（开发环境统一使用）
4. 安装过程中确保勾选 "MySQL Server 8.0"

**创建项目数据库**：

```sql
-- 使用 MySQL Workbench 或命令行连接
mysql -u root -p

CREATE DATABASE library_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- 创建开发用户（可选，用于避免 root 直连）
CREATE USER 'library_dev'@'localhost' IDENTIFIED BY 'dev123456';
GRANT ALL PRIVILEGES ON library_db.* TO 'library_dev'@'localhost';
FLUSH PRIVILEGES;

EXIT;
```

**验证**：

```bash
mysql -u root -p -e "SELECT VERSION();"
# 预期输出：8.0.x
```

### 3.2 Redis 7.2（手动安装）

**Windows 安装**：

Redis 官方不支持 Windows，使用 Memurai 或 WSL 方案：

```powershell
# 方式一：通过 WSL2 安装（推荐）
wsl --install -d Ubuntu
wsl sudo apt update && sudo apt install redis-server -y
wsl sudo sed -i 's/supervised no/supervised systemd/' /etc/redis/redis.conf
wsl sudo systemctl enable redis-server
wsl sudo systemctl start redis-server

# 方式二：Memurai（Windows 原生 Redis 替代品）
scoop bucket add versions
scoop install memurai
```

**配置密码**：

编辑 `redis.conf`（WSL：`/etc/redis/redis.conf`）：

```
requirepass redis123456
```

**验证**：

```bash
redis-cli -a redis123456 PING
# 预期输出：PONG
```

### 3.3 Elasticsearch 8.11（手动安装）

**Windows 安装**：

1. 从 https://www.elastic.co/downloads/elasticsearch 下载 8.11.x Windows ZIP
2. 解压到 `C:\tools\elasticsearch-8.11.0`
3. 编辑 `config\elasticsearch.yml`：

```yaml
cluster.name: library-es
node.name: node-1
path.data: data
path.logs: logs
network.host: 127.0.0.1
http.port: 9200

# 开发环境禁用安全（生产环境务必启用！）
xpack.security.enabled: false
xpack.security.enrollment.enabled: false
xpack.security.http.ssl.enabled: false
xpack.security.transport.ssl.enabled: false
```

4. 安装 IK 中文分词器：

```bash
cd C:\tools\elasticsearch-8.11.0
bin\elasticsearch-plugin.bat install https://get.infini.cloud/elasticsearch/analysis-ik/8.11.0
```

5. 启动：

```bash
bin\elasticsearch.bat
```

**验证**：

```bash
curl http://localhost:9200
# 预期：返回 JSON 含 "cluster_name": "library-es"
```

### 3.4 Neo4j 5.x（手动安装）

**Windows 安装**：

1. 从 https://neo4j.com/download-center/#community 下载 Neo4j Community 5.x
2. 安装完成后启动 Neo4j Desktop
3. 创建本地数据库：
   - Database Name: `library-kg`
   - Password: `neo4j123456`
   - Version: 5.x
4. 启动数据库

**验证**：

浏览器访问 http://localhost:7474，使用 `neo4j / neo4j123456` 登录。

**安装 APOC 和 GDS 插件**（图谱算法需要）：

在 Neo4j Desktop → 数据库 → Plugins → 安装 APOC 和 Graph Data Science Library。

### 3.5 RabbitMQ 3.12（手动安装）

**Windows 安装**：

```powershell
# Scoop
scoop install rabbitmq

# 或从 https://www.rabbitmq.com/install-windows.html 下载安装
```

**启动管理插件**：

```bash
rabbitmq-plugins enable rabbitmq_management
# 管理界面：http://localhost:15672
# 默认账号：guest / guest
```

**验证**：

浏览器访问 http://localhost:15672，使用 `guest/guest` 登录。

---

## 4. 后端项目启动

### 4.1 克隆项目

```bash
git clone <仓库地址> library-system
cd library-system
```

### 4.2 配置环境变量

在项目根目录创建 `.env` 文件（已加入 `.gitignore`）：

```properties
# ========== 数据库 ==========
DB_HOST=localhost
DB_PORT=3306
DB_NAME=library_db
DB_USERNAME=root
DB_PASSWORD=root123456

# ========== Redis ==========
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis123456

# ========== Elasticsearch ==========
ES_HOST=localhost
ES_PORT=9200

# ========== Neo4j ==========
NEO4J_URI=bolt://localhost:7687
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=neo4j123456

# ========== RabbitMQ ==========
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest

# ========== JWT ==========
# 生成方式：在终端执行以下命令生成一个安全的密钥
#   openssl rand -base64 64
# 或在 PowerShell 中：
#   [Convert]::ToBase64String((1..64 | ForEach-Object { Get-Random -Maximum 256 }) -as [byte[]])
# 注意：密钥长度至少 256 位（32 字节 Base64 编码后约 44 字符），此处使用 64 字节
JWT_SECRET=

# ========== LLM & Embedding ==========
# DeepSeek API（用于谈判策略生成、NER/RE、推荐理由等语言智能任务）
# 申请地址：https://platform.deepseek.com/
DEEPSEEK_API_KEY=
DEEPSEEK_BASE_URL=https://api.deepseek.com

# 阿里云百炼 Embedding API（用于语义检索增强、文本向量化）
# 申请地址：https://bailian.console.aliyun.com/
DASHSCOPE_API_KEY=
DASHSCOPE_EMBEDDING_MODEL=text-embedding-v3

# ========== Logging ==========
LOG_LEVEL=DEBUG
```

### 4.3 初始化数据库

首次启动时，Flyway 会自动执行数据库迁移脚本。基线迁移脚本 `V1__init_schema.sql` 已创建于 `library-server/library-bootstrap/src/main/resources/db/migration/`。后续各业务模块的完整 DDL 将在对应 feature 分支中补充。

也可以通过 Maven 手动执行：

```bash
cd library-server
mvn flyway:migrate -pl library-bootstrap
```

> **注意**：Docker Compose 中 MySQL 容器挂载了 `./docs/db/init.sql` 作为初始化脚本。该文件尚未创建（`V1__init_schema.sql` 已由 Flyway 管理），若需 Docker 启动时预置数据，可在 `docs/db/` 下创建 `init.sql`。

### 4.4 IDE 打开（IntelliJ IDEA）

1. **File → Open** → 选择 `library-server/` 目录下的 `pom.xml`（以 Maven 项目方式打开）
2. IDEA 会自动识别多模块结构，等待 Maven 依赖下载完成
3. 配置 JDK：**File → Project Structure → Project SDK** → 选择 JDK 17
4. 找到 `library-bootstrap/src/main/java/com/library/LibraryApplication.java`
5. 右键 → **Run 'LibraryApplication'**

### 4.5 命令行启动

```bash
# 跳过测试编译
mvn clean compile -DskipTests

# 启动应用（开发环境）
cd library-server
mvn spring-boot:run -pl library-bootstrap -Dspring-boot.run.profiles=dev

# 或者先打包再启动
mvn clean package -DskipTests
java -jar library-bootstrap/target/library-bootstrap-1.0.0.jar --spring.profiles.active=dev
```

### 4.6 验证后端

```bash
# 健康检查
curl http://localhost:8080/api/v1/health
# 预期输出：
# {
#   "status": "UP",
#   "components": {
#     "mysql": "UP",
#     "redis": "UP",
#     "elasticsearch": "UP",
#     "neo4j": "UP",
#     "rabbitmq": "UP"
#   }
# }

# 测试登录接口
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123456"}'

# Swagger UI（API 在线文档）
# 浏览器访问：http://localhost:8080/swagger-ui.html
```

### 4.7 初始化测试数据

```bash
# 执行数据初始化脚本（插入测试用户、图书、借阅记录等）
curl -X POST http://localhost:8080/api/v1/admin/init-test-data
```

---

## 5. Android 项目启动

### 5.1 安装 Android Studio

1. 从 https://developer.android.com/studio 下载 Android Studio Hedgehog (2023.1.1)+
2. 安装时勾选：
   - Android SDK
   - Android SDK Platform 34
   - Android Virtual Device (AVD)

### 5.2 配置 SDK

打开 Android Studio → **Settings → Appearance & Behavior → System Settings → Android SDK**：

- **SDK Platforms** 选项卡：勾选 API 34
- **SDK Tools** 选项卡：勾选 Android SDK Build-Tools 34, Android Emulator

### 5.3 导入项目

1. **File → Open** → 选择 `library-android/` 目录
2. 等待 Gradle Sync 完成
3. 若遇同步失败，检查 `local.properties` 中的 SDK 路径：

```properties
sdk.dir=C\:\\Users\\<你的用户名>\\AppData\\Local\\Android\\Sdk
```

### 5.4 配置后端地址

编辑 `library-android/app/src/main/java/com/library/android/config/ApiConfig.java`：

```java
public class ApiConfig {
    // 本地开发后端地址
    // Android 模拟器使用 10.0.2.2 访问宿主机的 localhost
    public static final String BASE_URL = "http://10.0.2.2:8080/";

    // 真机调试时改为电脑局域网 IP（确保同一 WiFi 下）
    // public static final String BASE_URL = "http://192.168.x.x:8080/";
}
```

### 5.5 运行

1. 点击工具栏的绿色 ▶ 按钮（Run 'app'）
2. 选择目标设备：模拟器 (AVD) 或 USB 连接的真机
3. 等待编译 → 安装 → 启动

### 5.6 验证

1. App 成功启动后显示登录界面
2. 输入测试账号 `2024001001 / Abc@123456` 登录
3. 进入首页后尝试搜索"Java"查看搜索结果

---

## 6. 验证清单

逐一确认以下项目，全部通过即环境搭建成功：

| # | 验证项 | 验证方法 | 预期结果 |
|---|--------|----------|----------|
| 1 | JDK 17 | `java --version` | 17.0.x |
| 2 | Maven | `mvn --version` | 3.9.x |
| 3 | MySQL | `mysql -u root -p -e "SELECT 1"` | 输出 `1` |
| 4 | Redis | `redis-cli -a password PING` | `PONG` |
| 5 | Elasticsearch | `curl localhost:9200` | JSON 响应，含 `cluster_name` |
| 6 | Neo4j | 浏览器 http://localhost:7474 | 可登录 |
| 7 | RabbitMQ | 浏览器 http://localhost:15672 | 管理界面可登录 |
| 8 | 后端编译 | `mvn clean compile` | BUILD SUCCESS |
| 9 | 后端启动 | 启动 `LibraryApplication` | 日志无 ERROR |
| 10 | 健康检查 | `curl localhost:8080/api/v1/health` | 所有组件 UP |
| 11 | Swagger | 浏览器 http://localhost:8080/swagger-ui.html | 显示 API 文档 |
| 12 | 登录接口 | POST `/api/v1/auth/login` | 返回 JWT Token |
| 13 | Android 编译 | Android Studio `Run 'app'` | BUILD SUCCESSFUL |
| 14 | Android 登录 | App 输入测试账号登录 | 进入主界面 |

---

## 7. 常见问题排查

### 7.1 Maven 依赖下载失败

**现象**：`Could not resolve dependencies` 或 `Failed to transfer`

**解决**：

```bash
# 1. 确认 settings.xml 配置了阿里云镜像（见 2.2 节）
# 2. 清除本地仓库缓存后重试
rm -rf ~/.m2/repository/com/library
mvn clean compile -U
```

### 7.2 Elasticsearch 启动失败（Windows）

**现象**：`java.lang.RuntimeException: can not run elasticsearch as root`

**解决**：不要以管理员身份运行，或以普通用户运行 `bin\elasticsearch.bat`。

**现象**：`max file descriptors [4096] for elasticsearch process is too low`

**解决**：在 `config\elasticsearch.yml` 中追加：

```yaml
# 减少资源需求
discovery.type: single-node
xpack.ml.enabled: false
```

### 7.3 Neo4j 连接失败 (bolt://localhost:7687)

**现象**：后端日志 `Unable to connect to localhost:7687`

**解决**：
1. 确认 Neo4j Desktop 中数据库已启动（绿色圆点）
2. 确认密码无误，默认 `neo4j123456`
3. 检查 Windows 防火墙是否阻止了 7687 端口

### 7.4 端口冲突

**现象**：`Port 8080 is already in use`

**解决**：

```bash
# Windows 查看端口占用
netstat -ano | findstr :8080
# 记下 PID，在任务管理器中结束该进程
# 或修改 application-dev.yml 中的 server.port
```

### 7.5 Android Studio 模拟器无法访问后端

**现象**：App 登录报 `java.net.ConnectException`

**解决**：
- 模拟器使用 `10.0.2.2` 代替 `localhost`（Android 模拟器的特殊地址）
- 真机调试确保手机和电脑在同一 WiFi，使用电脑的局域网 IP
- 检查 Windows 防火墙是否阻止了 8080 入站连接

### 7.6 MySQL 字符集问题

**现象**：插入中文数据出现乱码

**解决**：

```sql
-- 确认数据库字符集
SHOW CREATE DATABASE library_db;
-- 应显示 CHARACTER SET utf8mb4

-- 如不是，重新创建
ALTER DATABASE library_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 8. 项目结构速览

> **✅ 项目状态**：初始化框架已搭建完成（2026-06-15），Maven 多模块编译通过（7/7），Android 项目骨架就绪。以下目录树为系统架构设计所定义的**目标结构**，已与当前代码库一致。开发者可直接按此结构进行编码。

```
LibrarySystem-SIT/
├── docs/                                    # 📄 项目文档
│   ├── 系统架构设计文档.md                    #   架构蓝本
│   ├── CONTRIBUTING.md                      #   贡献指南
│   ├── api/
│   │   └── library-api.yaml                 #   OpenAPI 3.0 规范
│   └── DEVELOPMENT.md                       #   本文档
│
├── library-server/                          # ☕ 后端（Maven 多模块项目）
│   ├── pom.xml                              #   父 POM（依赖管理 + 插件管理）
│   ├── library-common/                      #   📦 公共模块
│   │   └── src/main/java/com/library/common/
│   │       ├── exception/                   #     全局异常 + 错误码枚举
│   │       ├── result/                      #     Result<T> + PageResult
│   │       ├── dto/                         #     公共 DTO
│   │       ├── utils/                       #     工具类
│   │       └── annotation/                  #     自定义注解
│   ├── library-ai/                          #   📦 AI 基础设施模块
│   │   └── src/main/java/com/library/ai/
│   │       ├── llm/                         #     DeepSeek API 封装
│   │       ├── embedding/                   #     百炼 Embedding 封装
│   │       ├── nlp/                         #     HanLP 本地 NLP
│   │       └── config/                      #     AI 模块配置
│   ├── library-core/                        #   📦 核心业务模块
│   │   └── src/main/java/com/library/core/
│   │       ├── controller/                  #     REST 控制器
│   │       ├── service/                     #     业务逻辑层
│   │       ├── mapper/                      #     MyBatis-Plus Mapper
│   │       ├── entity/                      #     数据库实体
│   │       ├── repository/                  #     ES / Redis 数据访问
│   │       ├── event/                       #     领域事件
│   │       └── config/                      #     模块配置
│   ├── library-knowledge-graph/             #   📦 知识图谱模块
│   │   └── src/main/java/com/library/kg/
│   │       ├── controller/                  #     知识图谱 API
│   │       ├── service/                     #     图谱构建 / 查询 / 溯源
│   │       ├── repository/                  #     Neo4j Cypher 查询
│   │       ├── model/                       #     图节点 / 关系模型
│   │       └── config/                      #     Neo4j 配置
│   ├── library-acquisition/                 #   📦 智能采编模块
│   │   └── src/main/java/com/library/acquisition/
│   │       ├── controller/                  #     采编 API
│   │       ├── service/                     #     预测 / 查重 / 谈判
│   │       ├── ml/                          #     ML 模型（简化 ARIMA）
│   │       └── repository/                  #     采编数据访问
│   ├── library-security/                    #   📦 安全模块
│   │   └── src/main/java/com/library/security/
│   │       ├── filter/                      #     JWT 认证过滤器
│   │       ├── handler/                     #     认证 / 授权处理器
│   │       └── config/                      #     Spring Security 配置
│   └── library-bootstrap/                   #   📦 启动模块（聚合入口）
│       └── src/main/
│           ├── java/com/library/
│           │   ├── LibraryApplication.java  #     🚀 Spring Boot 启动类
│           │   └── config/                  #     全局配置
│           └── resources/
│               ├── application.yml          #     公共配置
│               ├── application-dev.yml      #     开发环境配置
│               ├── application-prod.yml     #     生产环境配置
│               ├── application-test.yml     #     测试环境配置
│               ├── logback-spring.xml       #     日志配置
│               └── db/migration/            #     Flyway 迁移脚本
│
├── library-android/                         # 📱 Android 前端（独立 Gradle 项目）
│   ├── build.gradle.kts                     #   项目级 Gradle 构建
│   ├── settings.gradle.kts                  #   Gradle 设置
│   ├── gradle.properties                    #   Gradle 属性配置
│   ├── gradle/wrapper/                      #   Gradle Wrapper
│   ├── gradlew / gradlew.bat                #   Gradle 执行脚本
│   └── app/
│       ├── build.gradle.kts                 #   应用模块构建
│       ├── proguard-rules.pro               #   混淆规则
│       └── src/main/
│           ├── AndroidManifest.xml          #   应用清单
│           ├── java/com/library/android/
│           │   ├── LibraryApplication.java  #   Application 类
│           │   ├── ui/                      #   Activity / Fragment / Adapter
│           │   ├── viewmodel/               #   ViewModel
│           │   ├── repository/              #   数据仓库（网络→本地降级）
│           │   ├── network/                 #   Retrofit API 接口 + 拦截器
│           │   ├── model/                   #   数据模型（VO / DTO）
│           │   ├── di/                      #   Hilt 依赖注入模块
│           │   └── util/                    #   工具类
│           └── res/                         #   Android 资源
│               ├── layout/                  #     布局文件
│               ├── drawable/                #     图片资源
│               ├── values/                  #     字符串 / 主题 / 颜色
│               ├── navigation/              #     导航图
│               └── mipmap-*/                #     应用图标
│
├── docker-compose.yml                       # 🐳 Docker 中间件编排
├── .env.example                              # 🔑 环境变量模板
├── .editorconfig                             # 📝 跨编辑器代码风格
└── .gitignore                                # 🚫 Git 忽略规则
```

---

## 9. 日常开发命令

### 9.1 后端

```bash
# 进入后端项目目录
cd library-server

# 编译（跳过测试）
mvn clean compile -DskipTests

# 运行单元测试
mvn test

# 运行特定模块的测试
mvn test -pl library-core

# 运行单个测试类
mvn test -pl library-core -Dtest=BookServiceTest

# 打包
mvn clean package -DskipTests

# 代码格式化检查
mvn spotless:check

# 代码格式化自动修复
mvn spotless:apply

# 查看依赖树
mvn dependency:tree -pl library-core

# 查找过时依赖
mvn versions:display-dependency-updates
```

### 9.2 前端（Android）

```bash
# 在项目根目录
cd library-android

# Gradle 清理构建
./gradlew clean

# 编译 debug 版本
./gradlew assembleDebug

# 运行单元测试
./gradlew test

# 运行 lint 检查
./gradlew lint

# 安装到已连接设备
./gradlew installDebug
```

### 9.3 数据库

```bash
# 导出数据库（用于共享测试数据）
mysqldump -u root -p library_db > docs/db/backup/init_data.sql

# 导入数据库
mysql -u root -p library_db < docs/db/backup/init_data.sql

# 查看表结构
mysql -u root -p -e "USE library_db; SHOW TABLES; DESCRIBE book;"
```

### 9.4 Git 工作流

```bash
# 拉取最新代码
git pull origin main

# 创建功能分支
git checkout -b feature/your-feature-name

# 查看改动
git status
git diff

# 提交（遵循 Conventional Commits 规范）
git add -A
git commit -m "feat(core): 添加图书搜索接口"

# 推送
git push origin feature/your-feature-name
```

---

<a name="appendix-a"></a>

## 附录 A — docker-compose.yml

```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.0.35
    container_name: library-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root123456
      MYSQL_DATABASE: library_db
      MYSQL_CHARSET: utf8mb4
      MYSQL_COLLATION: utf8mb4_unicode_ci
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./docs/db/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-proot123456"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7.2-alpine
    container_name: library-redis
    command: redis-server --requirepass redis123456 --appendonly yes
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "redis123456", "PING"]
      interval: 10s
      timeout: 5s
      retries: 5

  elasticsearch:
    image: elasticsearch:8.11.0
    container_name: library-es
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms1g -Xmx1g"
      - xpack.security.enabled=false
      - xpack.security.enrollment.enabled=false
      - xpack.security.http.ssl.enabled=false
      - xpack.security.transport.ssl.enabled=false
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - es_data:/usr/share/elasticsearch/data
      - es_plugins:/usr/share/elasticsearch/plugins

  # IK 分词器安装初始化容器
  # 设计说明：es-ik-installer 先于 ES 启动，将 IK 插件安装到共享卷 es_plugins 中；
  # 随后 elasticsearch 挂载同一卷，启动时自动加载已安装的插件。
  # 首次安装后需重启 ES 容器：docker-compose restart elasticsearch
  es-ik-installer:
    image: elasticsearch:8.11.0
    container_name: library-es-ik-install
    command: >
      bash -c "
        if [ -d /usr/share/elasticsearch/plugins/analysis-ik ]; then
          echo 'IK plugin already installed, skipping.'
        else
          echo 'Installing IK plugin...'
          bin/elasticsearch-plugin install --batch https://get.infini.cloud/elasticsearch/analysis-ik/8.11.0
          echo 'IK plugin installed successfully. Please restart elasticsearch container.'
        fi
      "
    volumes:
      - es_plugins:/usr/share/elasticsearch/plugins
    depends_on:
      - elasticsearch

  neo4j:
    image: neo4j:5.17.0-community
    container_name: library-neo4j
    environment:
      NEO4J_AUTH: neo4j/neo4j123456
      NEO4J_PLUGINS: '["apoc", "graph-data-science"]'
    ports:
      - "7474:7474"
      - "7687:7687"
    volumes:
      - neo4j_data:/data
      - neo4j_logs:/logs
    healthcheck:
      test: ["CMD", "cypher-shell", "-u", "neo4j", "-p", "neo4j123456", "RETURN 1"]
      interval: 15s
      timeout: 10s
      retries: 10

  rabbitmq:
    image: rabbitmq:3.12-management-alpine
    container_name: library-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "check_port_connectivity"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  mysql_data:
  redis_data:
  es_data:
  es_plugins:
  neo4j_data:
  neo4j_logs:
  rabbitmq_data:
```

---

## 附录 B — 测试账号

| 角色 | 用户名 | 密码 | 用途 |
|------|--------|------|------|
| 学生 | `2024001001` | `Abc@123456` | 基础功能测试 |
| 教师 | `T20240001` | `Abc@123456` | 教师权限测试 |
| 图书管理员 | `librarian01` | `Abc@123456` | 管理功能测试 |
| 采编管理员 | `acquisitor01` | `Abc@123456` | 采编功能测试 |
| 系统管理员 | `admin` | `Admin@123456` | 全部权限 |

---

> **环境搭建完成后**，请回到[验证清单](#6-验证清单)逐项检查，全部通过后即可开始开发。
>
> **遇到问题？** 优先查阅 [第7节常见问题排查](#7-常见问题排查)，仍未解决则在项目群中提供完整的错误日志截图。
