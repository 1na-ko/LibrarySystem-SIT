package com.library.kg.service;

import com.library.kg.enums.TraceDirection;
import com.library.kg.vo.TraceGraphVO;

/**
 * 文献溯源服务.
 * <p>
 * 以某文献为起点，通过引用链（前向引用 / 后向引用 / 双向）构建文献演变关系图，
 * 揭示知识演化脉络。支持 BFS 多跳遍历与关键路径发现（Dijkstra）。
 * GDS 可用时优先 GDS 原生 Dijkstra，不可用时降级 Cypher 内置 {@code shortestPath()}.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface LiteratureTracingService {

    /**
     * 多跳溯源查询（BFS 遍历引用图）.
     *
     * @param bookId    起始图书 ID
     * @param direction 溯源方向（FORWARD/BACKWARD/BOTH）
     * @param maxDepth  最大跳数（1-5，超出截断）
     * @return 溯源图（含起始图书 + 多跳路径的节点和边，LIMIT 200）
     */
    TraceGraphVO trace(Long bookId, TraceDirection direction, int maxDepth);

    /**
     * 关键路径发现：寻找从源文献到目标文献的最优引用路径.
     * <p>
     * 使用 Dijkstra 算法（GDS）或 Cypher {@code shortestPath()}（降级），
     * 权重 = 引用次数倒数（高引用 = 短路径）.
     *
     * @param fromBookId 起始图书 ID
     * @param toBookId   目标图书 ID
     * @return 关键路径（无路径返回 {@code null}）
     */
    TraceGraphVO findKeyPath(Long fromBookId, Long toBookId);
}
