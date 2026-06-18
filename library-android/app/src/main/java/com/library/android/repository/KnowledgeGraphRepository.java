package com.library.android.repository;

import com.library.android.model.*;
import com.library.android.network.LibraryApi;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

/**
 * 知识图谱 Repository（人员 B 主导）.
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
                api.getBookGraph(bookId, depth).execute().body());
    }

    /** 文献溯源. */
    public Single<Result<TraceGraph>> traceLiterature(long bookId, String direction, int maxDepth) {
        return Single.fromCallable(() ->
                api.traceLiterature(bookId, direction, maxDepth).execute().body());
    }

    /** 学科主题网络. */
    public Single<Result<KnowledgeGraphVO>> getSubjectNetwork(String subjectName, int topK) {
        return Single.fromCallable(() ->
                api.getSubjectNetwork(subjectName, topK).execute().body());
    }

    /** 知识实体搜索（后端返回 KnowledgeGraphVO，此处转换为 List&lt;EntitySearchResult&gt;）. */
    public Single<Result<List<EntitySearchResult>>> searchEntities(String entity, String type) {
        return Single.fromCallable(() -> {
            Result<KnowledgeGraphVO> result = api.searchEntities(entity, type).execute().body();
            if (result == null || !result.isSuccess() || result.getData() == null) {
                // 将 KnowledgeGraphVO 的 Result 转换为 List 的 Result
                Result<List<EntitySearchResult>> converted = new Result<>();
                if (result != null) {
                    converted.setCode(result.getCode());
                    converted.setMessage(result.getMessage());
                }
                return converted;
            }
            KnowledgeGraphVO graph = result.getData();
            List<EntitySearchResult> entities = new java.util.ArrayList<>();
            if (graph.getNodes() != null) {
                for (GraphNode node : graph.getNodes()) {
                    EntitySearchResult entityResult = new EntitySearchResult();
                    entityResult.setEntityId(node.getId());
                    entityResult.setEntityName(node.getLabel());
                    entityResult.setEntityType(node.getType());
                    if (node.getProperties() != null && node.getProperties().containsKey("pagerank")) {
                        Object pr = node.getProperties().get("pagerank");
                        entityResult.setPagerank(pr instanceof Number ? ((Number) pr).doubleValue() : 0.0);
                    }
                    entities.add(entityResult);
                }
            }
            Result<List<EntitySearchResult>> converted = new Result<>();
            converted.setCode(result.getCode());
            converted.setMessage(result.getMessage());
            converted.setData(entities);
            return converted;
        });
    }
}
