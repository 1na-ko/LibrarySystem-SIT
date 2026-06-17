package com.library.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 10.6 采编全流程集成测试（P0）.
 * <p>
 * 预测→查重→缺口分析→创建谈判→获取建议。LLM 降级本地模板（test profile 禁用 DeepSeek）。
 * V100 种子：50 条借阅（供 ARIMA）+ 2 供应商(1001/1002) + 2 电子资源(1001/1002)。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("10.6 采编全流程")
class AcquisitionFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("预测→查重→缺口分析→创建谈判→获取建议 全流程")
    void shouldCompleteAcquisitionFlowWhenAcquisitorCalls() {
        String token = loginHelper.login("test_acquisitor", "Test@123456");

        // 1. 采购预测（subjectId=1 计算机科学，基于 50 条借阅 ARIMA）
        ResponseEntity<Map> predictResp = restTemplate.exchange(
                API + "/acquisition/predict?subjectId=1&months=3", HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(predictResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 2. 查重
        Map<String, Object> dupReq = Map.of(
                "isbn", "978-7-111-99999-9", "title", "测试查重新书", "author", "测试作者");
        ResponseEntity<Map> dupResp = restTemplate.postForEntity(
                API + "/acquisition/duplicate-check", loginHelper.auth(token, dupReq), Map.class);
        assertThat(dupResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 3. 缺口分析
        ResponseEntity<Map> gapResp = restTemplate.exchange(
                API + "/acquisition/gap-analysis?subjectId=1", HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(gapResp.getStatusCode().is2xxSuccessful()).isTrue();

        // 4. 创建谈判（resourceId=1001, supplierId=1001 V100 种子）
        ResponseEntity<Map> negResp = restTemplate.postForEntity(
                API + "/acquisition/negotiation?resourceId=1001&supplierId=1001",
                loginHelper.auth(token), Map.class);
        assertThat(negResp.getStatusCode().is2xxSuccessful()).isTrue();
        Long negId = ((Number) ((Map<?, ?>) negResp.getBody().get("data")).get("id")).longValue();

        // 5. 获取建议（LLM 降级本地模板，非空）
        ResponseEntity<Map> sugResp = restTemplate.exchange(
                API + "/acquisition/negotiation/" + negId + "/suggestion",
                HttpMethod.GET, loginHelper.auth(token), Map.class);
        assertThat(sugResp.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
