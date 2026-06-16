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

    /** 知识实体搜索. */
    public Single<Result<List<EntitySearchResult>>> searchEntities(String entity, String type) {
        return Single.fromCallable(() ->
                api.searchEntities(entity, type).execute().body());
    }
}
