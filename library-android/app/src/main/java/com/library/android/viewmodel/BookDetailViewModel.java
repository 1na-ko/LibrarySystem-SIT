package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.library.android.model.BookDetailVO;
import com.library.android.model.BookRecommendVO;
import com.library.android.model.Result;
import com.library.android.repository.BookRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 图书详情页 ViewModel.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class BookDetailViewModel extends ViewModel {

    private static final String TAG = "BookDetailViewModel";

    private final BookRepository bookRepository;

    private final MutableLiveData<BookDetailVO> bookDetail = new MutableLiveData<>();
    private final MutableLiveData<List<BookRecommendVO>> relatedBooks = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    public BookDetailViewModel(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public LiveData<BookDetailVO> getBookDetail() { return bookDetail; }
    public LiveData<List<BookRecommendVO>> getRelatedBooks() { return relatedBooks; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> isLoading() { return loading; }

    public void loadBookDetail(long bookId) {
        loading.setValue(true);
        disposables.add(
            bookRepository.getBookDetail(bookId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        loading.setValue(false);
                        if (result.isSuccess() && result.getData() != null) {
                            bookDetail.setValue(result.getData());
                        } else {
                            errorMessage.setValue(result.getMessage());
                        }
                    },
                    throwable -> {
                        loading.setValue(false);
                        Log.e(TAG, "加载图书详情失败", throwable);
                        errorMessage.setValue("加载失败：" + throwable.getMessage());
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

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}