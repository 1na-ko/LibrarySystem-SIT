package com.library.ai.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * 阿里云百炼 DashScope Embedding 客户端配置.
 * <p>
 * 读取 {@code ai.dashscope.*} 配置项，创建专用于 DashScope Embedding API 的 {@link WebClient} Bean。
 * 仅在 {@code ai.dashscope.api-key} 非空时创建，避免未配置 API Key 时阻塞应用启动。
 * <p>
 * 模型默认 text-embedding-v3（1024 维），单次请求最多 25 条文本。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class EmbeddingConfig {

    @Value("${ai.dashscope.api-key:}")
    private String apiKey;

    @Getter
    @Value("${ai.dashscope.embedding-model:text-embedding-v3}")
    private String embeddingModel;

    @Value("${ai.dashscope.base-url:https://dashscope.aliyuncs.com}")
    private String baseUrl;

    @Value("${ai.dashscope.connect-timeout:10s}")
    private Duration connectTimeout;

    @Getter
    @Value("${ai.dashscope.read-timeout:30s}")
    private Duration readTimeout;

    @Getter
    @Value("${ai.dashscope.write-timeout:20s}")
    private Duration writeTimeout;

    @Getter
    @Value("${ai.dashscope.max-retries:2}")
    private int maxRetries;

    @Getter
    @Value("${ai.dashscope.max-batch-size:25}")
    private int maxBatchSize;

    /**
     * 创建 DashScope Embedding API 专用 WebClient.
     * <p>
     * 仅在 {@code ai.dashscope.api-key} 非空时创建 Bean。
     */
    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${ai.dashscope.api-key:}')")
    public WebClient dashscopeWebClient() {
        HttpClient httpClient = AiHttpClientFactory.create(connectTimeout, readTimeout, writeTimeout);

        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        log.info("DashScope Embedding WebClient 已创建，目标地址: {}, 模型: {}", baseUrl, embeddingModel);
        return client;
    }
}
