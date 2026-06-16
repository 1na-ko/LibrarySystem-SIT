package com.library.core.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 客户端配置.
 * <p>
 * 手动声明 {@link ElasticsearchClient} Bean，读取 {@code spring.elasticsearch.uris}。
 * 不引入 spring-boot-starter-data-elasticsearch，仅使用底层 elasticsearch-java 8.11 客户端，
 * 避免不必要的自动配置与 Spring Data ES Repository 抽象层。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String uris;

    /**
     * 创建 ElasticsearchClient Bean.
     * <p>
     * 使用 {@link JacksonJsonpMapper}（由 elasticsearch-java 传递依赖提供）作为 JSON 映射器。
     * 连接池：最多 30 个总连接，每条路由最多 10 个。
     */
    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(HttpHost.create(uris))
                .setHttpClientConfigCallback(hc -> hc
                        .setMaxConnTotal(30)
                        .setMaxConnPerRoute(10))
                .build();

        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        ElasticsearchClient client = new ElasticsearchClient(transport);

        log.info("Elasticsearch 客户端已创建，目标地址: {}", uris);
        return client;
    }
}
