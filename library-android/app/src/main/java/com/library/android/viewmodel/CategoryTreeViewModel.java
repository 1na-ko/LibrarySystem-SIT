package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.CategoryVO;
import com.library.android.repository.BookRepository;
import com.library.android.ui.common.LoadingState;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 分类浏览 ViewModel — P1-01 新建.
 *
 * <p>原 CategoryTreeFragment 直接 {@code @Inject BookRepository} 违反 MVVM 分层，
 * 本 ViewModel 接管分类树加载，UI 仅观察 LiveData + LoadingState + errorEvent.
 *
 * @author LibrarySystem Team
 * @since 1.1.0
 */
@HiltViewModel
public class CategoryTreeViewModel extends BaseViewModel {

    private static final String TAG = "CategoryTreeViewModel";

    private final BookRepository bookRepository;

    private final MutableLiveData<List<CategoryVO>> categories = new MutableLiveData<>();

    @Inject
    public CategoryTreeViewModel(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public LiveData<List<CategoryVO>> getCategories() { return categories; }

    public void loadCategories() {
        setLoading(LoadingState.LOADING);
        disposables.add(bookRepository.getCategoryTree()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result != null && result.isSuccess() && result.getData() != null) {
                                List<CategoryVO> data = result.getData();
                                categories.setValue(data);
                                setLoading(data.isEmpty() ? LoadingState.EMPTY : LoadingState.CONTENT);
                            } else {
                                setLoading(LoadingState.ERROR);
                                postError(new RuntimeException(
                                        result != null ? result.getMessage() : "分类加载失败"));
                            }
                        },
                        throwable -> {
                            setLoading(LoadingState.ERROR);
                            Log.e(TAG, "加载分类树失败", throwable);
                            postError(throwable);
                        }
                ));
    }
}
