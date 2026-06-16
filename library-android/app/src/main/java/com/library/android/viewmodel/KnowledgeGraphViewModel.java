package com.library.android.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.library.android.model.EntitySearchResult;
import com.library.android.model.KnowledgeGraphVO;
import com.library.android.model.TraceGraph;
import com.library.android.repository.KnowledgeGraphRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 知识图谱 ViewModel（人员 B 主导）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class KnowledgeGraphViewModel extends ViewModel {

    private final KnowledgeGraphRepository repository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<KnowledgeGraphVO> bookGraph = new MutableLiveData<>();
    private final MutableLiveData<TraceGraph> traceGraph = new MutableLiveData<>();
    private final MutableLiveData<KnowledgeGraphVO> subjectNetwork = new MutableLiveData<>();
    private final MutableLiveData<List<EntitySearchResult>> entityResults = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    @Inject
    public KnowledgeGraphViewModel(KnowledgeGraphRepository repository) {
        this.repository = repository;
    }

    public LiveData<KnowledgeGraphVO> getBookGraph() { return bookGraph; }
    public LiveData<TraceGraph> getTraceGraph() { return traceGraph; }
    public LiveData<KnowledgeGraphVO> getSubjectNetwork() { return subjectNetwork; }
    public LiveData<List<EntitySearchResult>> getEntityResults() { return entityResults; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    /** 加载图书知识图谱. */
    public void loadBookGraph(long bookId, int depth) {
        loading.setValue(true);
        disposables.add(repository.getBookGraph(bookId, depth)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    loading.setValue(false);
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        bookGraph.setValue(result.getData());
                    } else {
                        errorMessage.setValue(result != null ? result.getMessage() : "图谱数据加载失败");
                    }
                }, throwable -> {
                    loading.setValue(false);
                    errorMessage.setValue(throwable.getMessage());
                }));
    }

    /** 文献溯源. */
    public void traceLiterature(long bookId, String direction, int maxDepth) {
        loading.setValue(true);
        disposables.add(repository.traceLiterature(bookId, direction, maxDepth)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    loading.setValue(false);
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        traceGraph.setValue(result.getData());
                    } else {
                        errorMessage.setValue("溯源数据加载失败");
                    }
                }, throwable -> {
                    loading.setValue(false);
                    errorMessage.setValue(throwable.getMessage());
                }));
    }

    /** 学科主题网络. */
    public void loadSubjectNetwork(String subjectName, int topK) {
        loading.setValue(true);
        disposables.add(repository.getSubjectNetwork(subjectName, topK)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    loading.setValue(false);
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        subjectNetwork.setValue(result.getData());
                    } else {
                        errorMessage.setValue("学科网络加载失败");
                    }
                }, throwable -> {
                    loading.setValue(false);
                    errorMessage.setValue(throwable.getMessage());
                }));
    }

    /** 知识实体搜索. */
    public void searchEntities(String entity, String type) {
        loading.setValue(true);
        disposables.add(repository.searchEntities(entity, type)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    loading.setValue(false);
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        entityResults.setValue(result.getData());
                    } else {
                        entityResults.setValue(null);
                    }
                }, throwable -> {
                    loading.setValue(false);
                    errorMessage.setValue(throwable.getMessage());
                }));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
