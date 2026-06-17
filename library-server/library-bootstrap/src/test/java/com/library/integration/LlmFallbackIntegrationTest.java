package com.library.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 10.9 LLM 降级验证集成测试（P1）.
 * <p>
 * test profile 禁用 DeepSeek（api-key 空）→ 推荐理由/谈判策略/NER 降级到本地模板，
 * 验证降级路径正常（不因 LLM 不可用而 500）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.9 LLM 降级验证")
class LlmFallbackIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("禁用DeepSeek后推荐应降级返回不报500")
    void shouldFallbackWhenLlmDisabledForRecommendation() {
        String token = loginHelper.login("test_student", "Test@123456");
        ResponseEntity<Map> resp = restTemplate.exchange(
                API + "/users/me/recommendations?limit=5", HttpMethod.GET, loginHelper.auth(token), Map.class);
        // 降级路径正常，未因 LLM 不可用而 500
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("禁用DeepSeek后谈判建议应降级为本地模板")
    void shouldFallbackToLocalStrategyWhenLlmDisabledForNegotiation() {
        String token = loginHelper.login("test_acquisitor", "Test@123456");
        ResponseEntity<Map> negResp = restTemplate.postForEntity(
                API + "/acquisition/negotiation?resourceId=1001&supplierId=1001",
                loginHelper.auth(token), Map.class);
        assertThat(negResp.getStatusCode().is2xxSuccessful()).isTrue();
        Long negId = ((Number) ((Map<?, ?>) negResp.getBody().get("data")).get("id")).longValue();

        ResponseEntity<Map> sugResp = restTemplate.exchange(
                API + "/acquisition/negotiation/" + negId + "/suggestion",
                HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(sugResp.getStatusCode().is2xxSuccessful()).isTrue();
        // 本地模板降级，建议非空
        assertThat(sugResp.getBody().get("data")).isNotNull();
    }
}
