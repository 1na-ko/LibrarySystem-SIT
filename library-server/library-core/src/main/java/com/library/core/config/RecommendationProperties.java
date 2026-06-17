package com.library.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 推荐引擎配置属性.
 * <p>
 * 对应 {@code application.yml} 中 {@code recommendation.*} 配置块，
 * 控制多路召回权重、并行超时和结果上限。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "recommendation")
public class RecommendationProperties {

    /** 协同过滤权重（默认 0.4） */
    private double cfWeight = 0.4;

    /** 内容推荐权重（默认 0.3） */
    private double contentWeight = 0.3;

    /** 知识图谱权重（默认 0.3） */
    private double kgWeight = 0.3;

    /** User-CF 相似用户 Top-K（默认 20） */
    private int userCfTopK = 20;

    /** Item-CF 每本书取相似书 Top-K（默认 10） */
    private int itemCfTopK = 10;

    /** 内容推荐候选池上限（默认 500，控制 Embedding API 调用量） */
    private int contentCandidateLimit = 500;

    /** 并行召回超时秒数（默认 5） */
    private long recallTimeoutSeconds = 5;

    /** 推荐结果条数上限（默认 50） */
    private int maxLimit = 50;
}
