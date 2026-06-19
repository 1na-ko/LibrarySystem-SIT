package com.library.android.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.EntitySearchResult;
import com.library.android.model.KnowledgeGraphVO;
import com.library.android.model.TraceGraph;
import com.library.android.repository.KnowledgeGraphRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 知识图谱 ViewModel（人员 B 主导）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class KnowledgeGraphViewModel extends BaseViewModel {

    private final KnowledgeGraphRepository repository;

    private final MutableLiveData<KnowledgeGraphVO> bookGraph = new MutableLiveData<>();
    private final MutableLiveData<TraceGraph> traceGraph = new MutableLiveData<>();
    private final MutableLiveData<TraceGraph> keyPath = new MutableLiveData<>();
    private final MutableLiveData<KnowledgeGraphVO> subjectNetwork = new MutableLiveData<>();
    private final MutableLiveData<List<EntitySearchResult>> entityResults = new MutableLiveData<>();
    @Inject
    public KnowledgeGraphViewModel(KnowledgeGraphRepository repository) {
        this.repository = repository;
    }

    public LiveData<KnowledgeGraphVO> getBookGraph() { return bookGraph; }
    public LiveData<TraceGraph> getTraceGraph() { return traceGraph; }
    /** WP-5：暴露关键路径 LiveData（KG 主页"关键路径"chip 入口使用）. */
    public LiveData<TraceGraph> getKeyPath() { return keyPath; }
    public LiveData<KnowledgeGraphVO> getSubjectNetwork() { return subjectNetwork; }
    public LiveData<List<EntitySearchResult>> getEntityResults() { return entityResults; }
    /** 加载图书知识图谱. */
    public void loadBookGraph(long bookId, int depth) {
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        disposables.add(repository.getBookGraph(bookId, depth)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        bookGraph.setValue(result.getData());
                    } else {
                        postError(new RuntimeException(result != null ? result.getMessage() : "图谱数据加载失败"));
                    }
                }, throwable -> {
                    setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                    postError(new RuntimeException(throwable.getMessage()));
                }));
    }

    /** 文献溯源. */
    public void traceLiterature(long bookId, String direction, int maxDepth) {
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        disposables.add(repository.traceLiterature(bookId, direction, maxDepth)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        traceGraph.setValue(result.getData());
                    } else {
                        postError(new RuntimeException("溯源数据加载失败"));
                    }
                }, throwable -> {
                    setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                    postError(new RuntimeException(throwable.getMessage()));
                }));
    }

    /** 学科主题网络. */
    public void loadSubjectNetwork(String subjectName, int topK) {
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        disposables.add(repository.getSubjectNetwork(subjectName, topK)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        subjectNetwork.setValue(result.getData());
                    } else {
                        postError(new RuntimeException("学科网络加载失败"));
                    }
                }, throwable -> {
                    setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                    postError(new RuntimeException(throwable.getMessage()));
                }));
    }

    /** 知识实体搜索. */
    public void searchEntities(String entity, String type) {
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        disposables.add(repository.searchEntities(entity, type)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        entityResults.setValue(result.getData());
                    } else {
                        entityResults.setValue(null);
                    }
                }, throwable -> {
                    setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                    postError(new RuntimeException(throwable.getMessage()));
                }));
    }

    /**
     * WP-5：关键路径发现（两本图书之间的语义最短路径）.
     *
     * @param bookId       源图书 ID（路径起点）
     * @param targetBookId 目标图书 ID（路径终点）
     */
    public void loadKeyPath(long bookId, long targetBookId) {
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        disposables.add(repository.getKeyPath(bookId, targetBookId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        keyPath.setValue(result.getData());
                    } else {
                        postError(new RuntimeException(result != null ? result.getMessage() : "关键路径加载失败"));
                    }
                }, throwable -> {
                    setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                    postError(new RuntimeException(throwable.getMessage()));
                }));
    }


}
