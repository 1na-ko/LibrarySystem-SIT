package com.library.kg.service;

import com.library.kg.vo.KnowledgeGraphVO;

/**
 * 知识图谱查询服务.
 * <p>
 * 以图书为中心查询 1-3 跳邻居节点与关系，用于前端力导向图可视化。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface GraphQueryService {

    /**
     * 获取以指定图书为中心的知识图谱.
     *
     * @param bookId 中心图书 ID
     * @param depth  查询深度（1-3 跳，超出自动截断到 3）
     * @return 节点 + 边集合（无数据返回空图，不抛异常）
     */
    KnowledgeGraphVO getBookGraph(Long bookId, int depth);

    /**
     * 模糊搜索知识图谱实体.
     *
     * @param entity 实体名称模糊搜索词
     * @param type   实体类型（可选），如 BOOK/AUTHOR/KEYWORD/SUBJECT
     * @return 匹配的图谱节点列表（按 PageRank 排序，含权重）
     */
    KnowledgeGraphVO searchEntities(String entity, String type);
}
