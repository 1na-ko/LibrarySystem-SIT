# =============================================================================
# 图书馆智能管理系统 — 本地一键验证脚本（阶段10）
# Windows 用 git bash / WSL 运行（make 需 GNU make 3.82+，支持 .RECIPEPREFIX）
# 集成测试需 docker-compose 预启动 5 中间件（MySQL/Redis/ES+IK/Neo4j/RabbitMQ）
# =============================================================================
.RECIPEPREFIX = >

.PHONY: up down test itest itest-tcp package coverage verify-all es-ik-check wait-middleware

# 启动本地开发中间件（集成测试与应用调试共用）
up:
>docker-compose up -d
>@echo "中间件已启动。ES 首次需安装 IK 分词器后重启：make es-ik-check"

down:
>docker-compose down

# ES IK 分词器安装检查与提示（首次安装后需 restart ES 才生效）
es-ik-check:
>@echo "检查 ES IK 插件..."
>@docker exec library-es ls /usr/share/elasticsearch/plugins/analysis-ik >/dev/null 2>&1 \
  && echo "✓ IK 已安装" \
  || (echo "⚠ IK 未安装，执行：docker-compose up -d es-ik-installer && docker-compose restart elasticsearch")

# 仅单元测试（快，日常开发，不需 Docker）
test:
>cd library-server && mvn test

# 等待 5 中间件端口就绪（启动前检查；若未启动则提示 make up）
wait-middleware:
>@echo "等待中间件健康..."
>@for svc in mysql:3306 redis:6379 elasticsearch:9200 neo4j:7687 rabbitmq:5672; do \
   port=$${svc##*:}; name=$${svc%%:*}; \
   for i in 1 2 3 4 5 6 7 8 9 10; do \
     (echo > /dev/tcp/localhost/$$port) 2>/dev/null && break; \
     echo "等待 $$name ($$port)..."; sleep 3; \
   done; \
   (echo > /dev/tcp/localhost/$$port) 2>/dev/null || { echo "✗ $$name ($$port) 未就绪，请先 make up"; exit 1; }; \
 done
>@echo "✓ 5 中间件就绪"

# 集成测试（需 docker-compose 5 中间件运行中，首次约 5-10 分钟）
itest: wait-middleware
>cd library-server && mvn test -Pintegration

# 集成测试（Docker Desktop 29 兼容方案：通过 TCP 2375 直连 Docker daemon
# 绕过 CLI 代理拦截；需在 Docker Desktop → Settings → General 勾选
# "Expose daemon on tcp://localhost:2375 without TLS"，
# 详见 docs/implementation/阶段10完成记录.md §5）
itest-tcp: wait-middleware
>cd library-server && DOCKER_HOST=tcp://localhost:2375 mvn test -Pintegration

# 打包（跳过测试）
package:
>cd library-server && mvn clean package -DskipTests

# 覆盖率报告（含集成测试，需 Docker；对照架构文档 §11.4 目标）
coverage: wait-middleware
>cd library-server && mvn verify -Pintegration
>@echo "聚合覆盖率报告：library-server/library-bootstrap/target/site/jacoco-aggregate/index.html"

# 一键全验证
verify-all: itest coverage
