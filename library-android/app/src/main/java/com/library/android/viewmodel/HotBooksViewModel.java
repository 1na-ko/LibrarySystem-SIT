package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.BookSimpleVO;
import com.library.android.model.CategoryVO;
import com.library.android.repository.BookRepository;
import com.library.android.ui.common.LoadingState;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 热门图书 ViewModel — P1-01 新建.
 *
 * <p>管理两路数据：分类下拉选择器数据 + 当前分类的热门图书列表.
 * 原 HotBooksFragment 直接 {@code @Inject BookRepository} 已下沉到此.
 *
 * @author LibrarySystem Team
 * @since 1.1.0
 */
@HiltViewModel
public class HotBooksViewModel extends BaseViewModel {

    private static final String TAG = "HotBooksViewModel";
    private static final int HOT_LIMIT = 50;

    private final BookRepository bookRepository;

    private final MutableLiveData<List<CategoryVO>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<BookSimpleVO>> hotBooks = new MutableLiveData<>();

    @Inject
    public HotBooksViewModel(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public LiveData<List<CategoryVO>> getCategories() { return categories; }
    public LiveData<List<BookSimpleVO>> getHotBooks() { return hotBooks; }

    public void loadCategories() {
        disposables.add(bookRepository.getCategoryTree()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result != null && result.isSuccess() && result.getData() != null) {
                                categories.setValue(result.getData());
                            } else {
                                postError(new RuntimeException(
                                        result != null ? result.getMessage() : "分类加载失败"));
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "加载分类失败", throwable);
                            postError(throwable);
                        }
                ));
    }

    /** 加载指定分类下的热门图书（categoryId 为 null 表示全部）. */
    public void loadHotBooks(@androidx.annotation.Nullable Long categoryId) {
        setLoading(LoadingState.LOADING);
        disposables.add(bookRepository.getHotBooks(categoryId, HOT_LIMIT)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> {
                            if (result != null && result.isSuccess() && result.getData() != null) {
                                List<BookSimpleVO> data = result.getData();
                                hotBooks.setValue(data);
                                setLoading(data.isEmpty() ? LoadingState.EMPTY : LoadingState.CONTENT);
                            } else {
                                setLoading(LoadingState.EMPTY);
                            }
                        },
                        throwable -> {
                            Log.e(TAG, "加载热门图书失败", throwable);
                            setLoading(LoadingState.EMPTY);
                            postError(throwable);
                        }
                ));
    }
}
