package com.library.core.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch 索引初始化器.
 * <p>
 * 启动时检查 {@code books} 索引是否存在，不存在则按架构文档 §5.3 的定义创建。
 * 使用 IK 分词器 + Completion Suggester，幂等（已存在则跳过），失败不阻塞启动。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsIndexInitializer {

    /** books 索引名称 */
    public static final String BOOKS_INDEX = "books";

    private final ElasticsearchClient esClient;

    @PostConstruct
    public void init() {
        try {
            boolean exists = esClient.indices()
                    .exists(req -> req.index(BOOKS_INDEX))
                    .value();

            if (exists) {
                log.info("ES 索引 [{}] 已存在，跳过创建", BOOKS_INDEX);
                return;
            }

            createIndex();
            log.info("ES 索引 [{}] 创建成功", BOOKS_INDEX);
        } catch (Exception e) {
            log.warn("ES 索引 [{}] 初始化失败，搜索功能可能不可用: {}", BOOKS_INDEX, e.getMessage());
        }
    }

    /**
     * 删除并重建 books 索引（供 EsRebuildJob 全量重建调用）.
     */
    public void recreateIndex() throws Exception {
        // 删除旧索引（不存在则忽略）
        try {
            esClient.indices().delete(d -> d.index(BOOKS_INDEX));
            log.info("ES 索引 [{}] 已删除", BOOKS_INDEX);
            // 等待删除传播
            Thread.sleep(1000);
        } catch (Exception e) {
            log.debug("ES 索引删除跳过（可能不存在）: {}", e.getMessage());
        }
        createIndex();
        log.info("ES 索引 [{}] 重建成功", BOOKS_INDEX);
    }

    /**
     * 创建 books 索引，含 IK 自定义分析器和 Completion Suggester.
     * <p>
     * Mapping 严格对齐架构文档 §5.3：
     * - title/author: text (ik_smart_analyzer) + keyword 子字段
     * - keywords: text (ik_smart_analyzer), boost 2.0
     * - suggest: completion 类型
     */
    private void createIndex() throws Exception {
        esClient.indices().create(req -> req
                .index(BOOKS_INDEX)
                .settings(s -> s
                        .analysis(analysis()))
                .mappings(m -> m
                        .properties("id", p -> p.long_(l -> l))
                        .properties("isbn", p -> p.keyword(k -> k))
                        .properties("title", p -> p
                                .text(t -> t.analyzer("ik_smart_analyzer")
                                        .fields("keyword", f -> f.keyword(k -> k))))
                        .properties("author", p -> p
                                .text(t -> t.analyzer("ik_smart_analyzer")
                                        .fields("keyword", f -> f.keyword(k -> k))))
                        .properties("publisher", p -> p.text(t -> t.analyzer("ik_smart_analyzer")))
                        .properties("description", p -> p.text(t -> t.analyzer("ik_smart_analyzer")))
                        .properties("keywords", p -> p
                                .text(t -> t.analyzer("ik_smart_analyzer").boost(2.0)))
                        .properties("categoryId", p -> p.long_(l -> l))
                        .properties("categoryName", p -> p.keyword(k -> k))
                        .properties("borrowCount", p -> p.integer(i -> i))
                        .properties("availCopies", p -> p.integer(i -> i))
                        .properties("coverUrl", p -> p.keyword(k -> k))
                        .properties("location", p -> p.keyword(k -> k))
                        .properties("pubDate", p -> p.date(d -> d))
                        .properties("suggest", p -> p.completion(c -> c.analyzer("ik_smart_analyzer")))
                )
        );
    }

    /**
     * 构建 IK 自定义分析器.
     */
    private IndexSettingsAnalysis analysis() {
        return IndexSettingsAnalysis.of(a -> a
                .analyzer("ik_smart_analyzer", analyzer -> analyzer
                        .custom(c -> c
                                .tokenizer("ik_smart")
                                .filter("lowercase")
                        )
                )
        );
    }
}
