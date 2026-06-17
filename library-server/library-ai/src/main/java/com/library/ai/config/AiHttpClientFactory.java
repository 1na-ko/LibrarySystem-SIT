package com.library.ai.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * AI 模块 HttpClient 共享工厂，消除 LlmConfig 与 EmbeddingConfig 中重复的
 * reactor-netty 超时配置样板代码（各约 15 行）。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public final class AiHttpClientFactory {

    private AiHttpClientFactory() {
        // 工具类，禁止实例化
    }

    /**
     * 创建配置好连接/读/写超时的 reactor-netty HttpClient.
     *
     * @param connectTimeout 连接超时
     * @param readTimeout    响应读取超时
     * @param writeTimeout   写入超时
     * @return 已配置超时的 HttpClient
     */
    public static HttpClient create(Duration connectTimeout, Duration readTimeout, Duration writeTimeout) {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
                .responseTimeout(readTimeout)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(readTimeout.toSeconds(), TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(writeTimeout.toSeconds(), TimeUnit.SECONDS)));
    }
}
