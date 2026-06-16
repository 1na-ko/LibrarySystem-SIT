package com.library;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot 上下文加载测试（集成测试）.
 * <p>
 * 需完整中间件（MySQL/Redis）与 {@code JWT_SECRET} 环境变量，属于集成测试范畴。
 * 阶段 1 遵循纯单元测试策略，暂时禁用；阶段 10 引入 Testcontainers 后启用。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Disabled("集成测试：需中间件 + JWT_SECRET，待阶段 10 Testcontainers 启用")
@SpringBootTest
class LibraryApplicationTests {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文正常加载
    }
}
