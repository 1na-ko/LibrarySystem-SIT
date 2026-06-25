package com.library;

import com.library.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Spring Boot 上下文加载冒烟测试（集成测试）.
 * <p>
 * 阶段 10 引入 Testcontainers 后启用：继承 {@link AbstractIntegrationTest}，验证 5 个中间件
 * 容器（MySQL/Redis/ES+IK/Neo4j/RabbitMQ）全部启动 + Spring 上下文正常加载 + RabbitMQ 事件总线
 * Exchange/Queue 声明成功。
 * <p>
 * 日常 {@code mvn test} 排除本类（surefire excludes 匹配 LibraryApplicationTests）；
 * {@code mvn test -Pintegration -Dtest=LibraryApplicationTests} 运行（首次约 15-30 分钟含镜像拉取）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
class LibraryApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文正常加载（5 容器 + 7 模块所有 Bean + RabbitMQ 事件总线声明）
    }
}
