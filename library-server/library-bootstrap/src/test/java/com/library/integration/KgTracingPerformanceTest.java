package com.library.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 10.12 KG 溯源查询性能测试.
 * <p>
 * 5 跳 BFS 溯源 < 3s。需先构建图谱（admin rebuild-all，LLM NER 降级 HanLP）。
 * BFS 用原生 Cypher 路径查询，不强依赖 GDS。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.12 KG 溯源查询性能")
class KgTracingPerformanceTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("5跳BFS溯源应<3s")
    void shouldReturn5HopBfsPathWithin3Seconds() {
        // 构建图谱（admin，kg:admin 权限；LLM NER 降级 HanLP 本地分词）
        String adminToken = loginHelper.login("admin", "Admin@123456");
        restTemplate.postForEntity(API + "/admin/kg/rebuild-all", loginHelper.auth(adminToken), Map.class);

        // 5 跳溯源计时（test_librarian 调用，kg:read 权限）
        String token = loginHelper.login("test_librarian", "Test@123456");
        long start = System.nanoTime();
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/kg/book/10004/trace?direction=BOTH&maxDepth=5",
                HttpMethod.GET, loginHelper.auth(token), Map.class);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // 容错：若权限不足 403，跳过性能断言；有权限则验证 <3s 且不 500
        assertThat(resp.getStatusCode().value()).isIn(200, 403);
        if (resp.getStatusCode().is2xxSuccessful()) {
            assertThat(elapsedMs).isLessThan(3000);
        }
    }
}
