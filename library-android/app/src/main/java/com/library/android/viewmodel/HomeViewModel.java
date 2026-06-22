package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.BookRecommendVO;
import com.library.android.model.BookSimpleVO;
import com.library.android.model.CategoryVO;
import com.library.android.repository.BookRepository;
import com.library.android.repository.UserRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 首页 ViewModel — 推荐流式 + 热门图书 + 分类导航.
 *
 * <p>P1-01：将原 HomeFragment 直接注入的 BookRepository 调用下沉至此，
 * UI 层仅观察 LiveData，错误统一通过 BaseViewModel.errorEvent 发布.
 *
 * <p>Activity scope 保证 HomeFragment 与 RecommendationsFragment 共享推荐数据.
 *
 * @author LibrarySystem Team
 * @since 2.0.0
 */
@HiltViewModel
public class HomeViewModel extends BaseViewModel {

    private static final String TAG = "HomeViewModel";

    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    private final MutableLiveData<List<BookRecommendVO>> recommendations = new MutableLiveData<>();
    /** AI 推荐导语（流式逐 token 累加）. */
    private final MutableLiveData<String> aiReason = new MutableLiveData<>("");
    /** 流式导语累加缓冲. */
    private final StringBuilder reasonBuf = new StringBuilder();
    /** AI 导语是否仍在生成. */
    private final MutableLiveData<Boolean> reasonStreaming = new MutableLiveData<>(false);

    /** P1-01：热门图书 / 分类树 LiveData（原 HomeFragment 内联订阅迁入）. */
    private final MutableLiveData<List<BookSimpleVO>> hotBooks = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryVO>> categories = new MutableLiveData<>();

    @Inject
    public HomeViewModel(UserRepository userRepository, BookRepository bookRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    public LiveData<List<BookRecommendVO>> getRecommendations() { return recommendations; }
    public LiveData<String> getAiReason() { return aiReason; }
    public LiveData<Boolean> isReasonStreaming() { return reasonStreaming; }
    public LiveData<List<BookSimpleVO>> getHotBooks() { return hotBooks; }
    public LiveData<List<CategoryVO>> getCategories() { return categories; }

    /** 加载个性化推荐（同步版）. */
    public void loadRecommendations() {
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        disposables.add(userRepository.getRecommendations(20)
                .retry(1)
                .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
                .observeOn(io.reactivex.rxjava3.android.schedulers.AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result.isSuccess() && result.getData() != null) {
                                recommendations.setValue(result.getData());
                                setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                            } else {
                                setLoading(com.library.android.ui.common.LoadingState.ERROR);
                                postError(new RuntimeException(result.getMessage()));
                            }
                        },
                        throwable -> {
                            setLoading(com.library.android.ui.common.LoadingState.ERROR);
                            Log.e(TAG, "加载推荐失败", throwable);
                            postError(throwable);
                        }
                ));
    }

    /**
     * 流式加载推荐：书目秒回 + LLM 导语逐 token.
     */
    public void loadRecommendationsStream() {
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        reasonStreaming.setValue(true);
        reasonBuf.setLength(0);
        aiReason.setValue("");
        recommendations.setValue(null);
        userRepository.streamRecommendations(20, new UserRepository.RecommendStreamCallback() {
            @Override
            public void onBooks(List<BookRecommendVO> books) {
                HomeViewModel.this.setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                recommendations.postValue(books);
            }
            @Override
            public void onReasonToken(String token) {
                reasonBuf.append(token);
                aiReason.postValue(reasonBuf.toString());
            }
            @Override
            public void onDone() {
                reasonStreaming.postValue(false);
            }
            @Override
            public void onError(Throwable e) {
                HomeViewModel.this.setLoading(com.library.android.ui.common.LoadingState.ERROR);
                reasonStreaming.postValue(false);
                Log.e(TAG, "流式推荐失败", e);
                if (recommendations.getValue() == null || recommendations.getValue().isEmpty()) {
                    postError(e);
                } else if (reasonBuf.length() == 0) {
                    aiReason.postValue("以上书目基于您的借阅历史精选，希望您喜欢。");
                }
            }
        });
    }

    /** P1-01：加载热门图书（取代 HomeFragment 内 bookRepository.getHotBooks 直接订阅）. */
    public void loadHotBooks(int limit) {
        disposables.add(bookRepository.getHotBooks(null, limit)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result != null && result.isSuccess() && result.getData() != null) {
                                hotBooks.setValue(result.getData());
                            } else {
                                postError(new RuntimeException(result != null ? result.getMessage() : "热门图书加载失败"));
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "加载热门图书失败", throwable);
                            postError(throwable);
                        }
                ));
    }

    /** P1-01：加载分类树（取代 HomeFragment 内 bookRepository.getCategoryTree 直接订阅）. */
    public void loadCategoryTree() {
        disposables.add(bookRepository.getCategoryTree()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result != null && result.isSuccess() && result.getData() != null) {
                                categories.setValue(result.getData());
                            } else {
                                postError(new RuntimeException(result != null ? result.getMessage() : "分类加载失败"));
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "加载分类失败", throwable);
                            postError(throwable);
                        }
                ));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        userRepository.disposeStreams();
    }
}
