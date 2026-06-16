package com.library.kg.service;

/**
 * 知识图谱构建服务.
 * <p>
 * 以 MySQL 馆藏图书为数据源，通过 NER（实体识别）→ RE（关系抽取）
 * → MERGE（写入 Neo4j）→ 实体对齐（合并同名节点）的流水线，
 * 将单本图书的元数据转化为知识图谱中的节点与关系。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface GraphBuildService {

    /**
     * 为指定图书构建知识图谱.
     * <p>
     * NER 阶段优先使用 LlmService（DeepSeek JSON Mode）识别作者/关键词/学科实体；
     * LLM 不可用时降级为 {@code NlpService.extractKeywords() + 规则切分}。
     * RE 阶段优先使用 LLM 判断关系类型；降级为共现统计建边。
     *
     * @param bookId MySQL 图书 ID
     * @throws com.library.common.exception.BizException KG_ENTITY_NOT_FOUND 图书不存在
     * @throws com.library.common.exception.BizException KG_BUILD_FAILED    Neo4j 写入失败
     */
    void buildGraph(Long bookId);

    /**
     * 全量重建所有馆藏图书的知识图谱.
     * <p>
     * 分页扫描 MySQL book 表，逐本调用 {@link #buildGraph(Long)}。
     *
     * @return 成功构建的图书数量
     */
    int rebuildAll();
}
