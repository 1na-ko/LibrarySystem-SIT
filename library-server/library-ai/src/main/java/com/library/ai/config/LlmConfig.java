package com.library.ai.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
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
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek LLM 客户端配置.
 * <p>
 * 读取 {@code ai.deepseek.*} 配置项，创建专用于 DeepSeek API 的 {@link WebClient} Bean。
 * 仅在 {@code ai.deepseek.api-key} 非空时创建，避免未配置 API Key 时阻塞应用启动。
 * <p>
 * 超时与重试策略：连接超时 10s / 读取超时 60s / 写入超时 30s，
 * 重试逻辑由 {@link com.library.ai.llm.LlmServiceImpl} 通过 reactor-retry 实现（最多 2 次）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class LlmConfig {

    @Value("${ai.deepseek.api-key:}")
    private String apiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Getter
    @Value("${ai.deepseek.model:deepseek-chat}")
    private String model;

    @Value("${ai.deepseek.connect-timeout:10s}")
    private Duration connectTimeout;

    @Getter
    @Value("${ai.deepseek.read-timeout:60s}")
    private Duration readTimeout;

    @Getter
    @Value("${ai.deepseek.max-retries:2}")
    private int maxRetries;

    @Getter
    @Value("${ai.deepseek.max-tokens:2048}")
    private int maxTokens;

    @Getter
    @Value("${ai.deepseek.temperature:0.7}")
    private double temperature;

    /**
     * 创建 DeepSeek API 专用 WebClient.
     * <p>
     * 仅在 {@code ai.deepseek.api-key} 非空时创建 Bean。
     * 使用 reactor-netty {@link HttpClient} 配置连接超时和读写超时。
     */
    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${ai.deepseek.api-key:}')")
    public WebClient deepseekWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
                .responseTimeout(readTimeout)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeout.toSeconds(), TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));

        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        log.info("DeepSeek WebClient 已创建，目标地址: {}/v1/chat/completions, 模型: {}", baseUrl, model);
        return client;
    }
}
