package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.BookDetailVO;
import com.library.android.model.BookRecommendVO;
import com.library.android.repository.BookRepository;
import com.library.android.ui.common.LoadingState;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 图书详情页 ViewModel（深度迁移：完全使用 BaseViewModel 的 errorEvent/loadingState）.
 */
@HiltViewModel
public class BookDetailViewModel extends BaseViewModel {

    private static final String TAG = "BookDetailViewModel";

    private final BookRepository bookRepository;

    private final MutableLiveData<BookDetailVO> bookDetail = new MutableLiveData<>();
    private final MutableLiveData<List<BookRecommendVO>> relatedBooks = new MutableLiveData<>();

    @Inject
    public BookDetailViewModel(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public LiveData<BookDetailVO> getBookDetail() { return bookDetail; }
    public LiveData<List<BookRecommendVO>> getRelatedBooks() { return relatedBooks; }

    public void loadBookDetail(long bookId) {
        setLoading(LoadingState.LOADING);
        disposables.add(
            bookRepository.getBookDetail(bookId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        if (result.isSuccess() && result.getData() != null) {
                            bookDetail.setValue(result.getData());
                            setLoading(LoadingState.CONTENT);
                        } else {
                            setLoading(LoadingState.ERROR);
                            postError(new RuntimeException(result.getMessage()));
                        }
                    },
                    throwable -> {
                        setLoading(LoadingState.ERROR);
                        Log.e(TAG, "加载图书详情失败", throwable);
                        postError(throwable);
                    }
                )
        );

        disposables.add(
            bookRepository.getRelatedBooks(bookId, 5)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        if (result.isSuccess() && result.getData() != null) {
                            relatedBooks.setValue(result.getData());
                        }
                    },
                    throwable -> Log.e(TAG, "加载相关推荐失败", throwable)
                )
        );
    }
}
