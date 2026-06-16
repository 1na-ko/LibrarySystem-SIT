package com.library.kg.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 知识图谱配置属性.
 * <p>
 * 对应 {@code application.yml} 中 {@code kg.*} 配置块，
 * 控制图谱查询深度、PageRank 参数、构建超时与 GDS 开关。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "kg")
public class KnowledgeGraphProperties {

    /** 默认图谱查询深度（1-3 跳，默认 2） */
    private int defaultQueryDepth = 2;

    /** 最大图谱查询深度（默认 3） */
    private int maxQueryDepth = 3;

    /** 文献溯源最大跳数（默认 5） */
    private int tracingMaxDepth = 5;

    /** 文献溯源结果上限（默认 200） */
    private int tracingLimit = 200;

    /** 主题网络 Jaccard 相似度阈值（默认 0.15） */
    private double topicJaccardThreshold = 0.15;

    /** 学科核心书目 Top-N（默认 50） */
    private int coreBookTopN = 50;

    /** PageRank 阻尼因子（默认 0.85） */
    private double pagerankDamping = 0.85;

    /** PageRank 迭代次数（默认 20） */
    private int pagerankIterations = 20;

    /** 图谱构建超时毫秒（默认 30000） */
    private long buildTimeoutMs = 30000;

    /** 是否启用 GDS 插件（默认 true，探测失败自动降级） */
    private boolean gdsEnabled = true;

    /** 启动期是否自动创建唯一约束（默认 true） */
    private boolean constraintsAutoCreate = true;
}
