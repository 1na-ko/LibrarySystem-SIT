package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.BookRecommendVO;
import com.library.android.repository.UserRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * 首页 ViewModel（WP4.1 新建）— 管理推荐流式加载，从 ProfileViewModel 拆分.
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

    private final MutableLiveData<List<BookRecommendVO>> recommendations = new MutableLiveData<>();
    /** AI 推荐导语（流式逐 token 累加）. */
    private final MutableLiveData<String> aiReason = new MutableLiveData<>("");
    /** 流式导语累加缓冲. */
    private final StringBuilder reasonBuf = new StringBuilder();
    /** AI 导语是否仍在生成. */
    private final MutableLiveData<Boolean> reasonStreaming = new MutableLiveData<>(false);

    @Inject
    public HomeViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<List<BookRecommendVO>> getRecommendations() { return recommendations; }
    public LiveData<String> getAiReason() { return aiReason; }
    public LiveData<Boolean> isReasonStreaming() { return reasonStreaming; }

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

    @Override
    protected void onCleared() {
        super.onCleared();
        userRepository.disposeStreams();
    }
}
