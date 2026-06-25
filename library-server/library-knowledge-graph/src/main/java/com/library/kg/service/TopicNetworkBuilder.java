package com.library.kg.service;

import com.library.kg.vo.KnowledgeGraphVO;

/**
 * 主题关联网络构建服务.
 * <p>
 * 基于关键词共现分析 + Jaccard 相似度构建学科主题关联网络，
 * 通过 PageRank 算法计算关键词中心度（控制可视化节点大小）。
 * GDS 可用时优先 Neo4j GDS 原生 PageRank，不可用时降级 Java 侧 power iteration。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface TopicNetworkBuilder {

    /**
     * 构建全局关键词主题网络.
     * <p>
     * 流程：提取所有 Keyword 节点共现关系 → 计算 Jaccard 相似度 →
     * 超过阈值的创建 {@code RELATED_TO} 边 → 运行 PageRank 计算中心度。
     */
    void buildTopicNetwork();

    /**
     * 构建指定学科主题网络（用于前端可视化）.
     *
     * @param subjectName 学科名称（Subject 节点 name 属性）
     * @param topK        返回关键词数量上限
     * @return 学科主题网络（节点 = 该学科下 Top-K 关键词，边 = RELATED_TO）
     */
    KnowledgeGraphVO buildSubjectNetwork(String subjectName, int topK);
}
