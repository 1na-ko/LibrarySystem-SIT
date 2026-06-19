package com.library.android.repository;

import com.library.android.model.*;
import com.library.android.network.ApiCallExecutor;
import com.library.android.network.LibraryApi;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Single;

/**
 * 知识图谱 Repository.
 *
 * <p>注：{@code searchEntities} 当前仍返回 {@code List<EntitySearchResult>} 兼容
 * 旧 EntitySearchFragment；阶段 B.7 会简化为直接返回 KnowledgeGraphVO.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class KnowledgeGraphRepository {

    private final LibraryApi api;

    public KnowledgeGraphRepository(LibraryApi api) {
        this.api = api;
    }

    /** 获取图书知识图谱. */
    public Single<Result<KnowledgeGraphVO>> getBookGraph(long bookId, int depth) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.getBookGraph(bookId, depth)));
    }

    /** 文献溯源. */
    public Single<Result<TraceGraph>> traceLiterature(long bookId, String direction, int maxDepth) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.traceLiterature(bookId, direction, maxDepth)));
    }

    /** 学科主题网络. */
    public Single<Result<KnowledgeGraphVO>> getSubjectNetwork(String subjectName, int topK) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.getSubjectNetwork(subjectName, topK)));
    }

    /** 关键路径查询（C.3 新增）— 两本图书之间的最短引用/关键词路径. */
    public Single<Result<TraceGraph>> getKeyPath(long bookId, long targetBookId) {
        return Single.fromCallable(() ->
                ApiCallExecutor.execute(api.getKeyPath(bookId, targetBookId)));
    }

    /** 知识实体搜索（节点扁平化为 EntitySearchResult 列表，便于列表 UI 渲染）. */
    public Single<Result<List<EntitySearchResult>>> searchEntities(String entity, String type) {
        return Single.fromCallable(() -> {
            Result<KnowledgeGraphVO> result = ApiCallExecutor.execute(api.searchEntities(entity, type));
            Result<List<EntitySearchResult>> converted = new Result<>();
            converted.setCode(result.getCode());
            converted.setMessage(result.getMessage());
            if (!result.isSuccess() || result.getData() == null) {
                return converted;
            }
            List<EntitySearchResult> entities = new ArrayList<>();
            KnowledgeGraphVO graph = result.getData();
            if (graph.getNodes() != null) {
                for (GraphNode node : graph.getNodes()) {
                    EntitySearchResult er = new EntitySearchResult();
                    er.setEntityId(node.getId());
                    er.setEntityName(node.getLabel());
                    er.setEntityType(node.getType());
                    if (node.getProperties() != null && node.getProperties().containsKey("pagerank")) {
                        Object pr = node.getProperties().get("pagerank");
                        er.setPagerank(pr instanceof Number ? ((Number) pr).doubleValue() : 0.0);
                    }
                    entities.add(er);
                }
            }
            converted.setData(entities);
            return converted;
        });
    }
}
