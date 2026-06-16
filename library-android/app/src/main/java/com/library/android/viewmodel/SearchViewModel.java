package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.library.android.model.BookVO;
import com.library.android.model.CategoryVO;
import com.library.android.model.PageResult;
import com.library.android.model.Result;
import com.library.android.repository.BookRepository;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 图书搜索页 ViewModel.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class SearchViewModel extends ViewModel {

    private static final String TAG = "SearchViewModel";
    private static final int PAGE_SIZE = 20;

    private final BookRepository bookRepository;

    private final MutableLiveData<List<BookVO>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<List<BookVO>> hotBooks = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryVO>> categories = new MutableLiveData<>();
    private final MutableLiveData<List<Map<String, String>>> suggestions = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(true);
    private final MutableLiveData<Integer> totalResults = new MutableLiveData<>(0);

    private final CompositeDisposable disposables = new CompositeDisposable();

    private String currentKeyword;
    private int currentPage = 1;

    @Inject
    public SearchViewModel(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public LiveData<List<BookVO>> getSearchResults() { return searchResults; }
    public LiveData<List<BookVO>> getHotBooks() { return hotBooks; }
    public LiveData<List<CategoryVO>> getCategories() { return categories; }
    public LiveData<List<Map<String, String>>> getSuggestions() { return suggestions; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> isLoading() { return loading; }
    public LiveData<Boolean> hasMore() { return hasMore; }
    public LiveData<Integer> getTotalResults() { return totalResults; }

    /** 加载首页数据（热门图书 + 分类导航）. */
    public void loadHomeData() {
        loading.setValue(true);
        disposables.add(
            bookRepository.getHotBooks(null, 10)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        if (result.isSuccess() && result.getData() != null) {
                            hotBooks.setValue(result.getData());
                        }
                    },
                    throwable -> Log.e(TAG, "加载热门图书失败", throwable)
                )
        );

        disposables.add(
            bookRepository.getCategoryTree()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        loading.setValue(false);
                        if (result.isSuccess() && result.getData() != null) {
                            categories.setValue(result.getData());
                        }
                    },
                    throwable -> {
                        loading.setValue(false);
                        Log.e(TAG, "加载分类失败", throwable);
                    }
                )
        );
    }

    /** 搜索图书（首次搜索或切换关键词）. */
    public void search(String keyword) {
        currentKeyword = keyword;
        currentPage = 1;
        hasMore.setValue(true);
        loading.setValue(true);

        disposables.add(
            bookRepository.searchBooks(keyword, null, null, null, currentPage, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        loading.setValue(false);
                        if (result.isSuccess() && result.getData() != null) {
                            PageResult<BookVO> page = result.getData();
                            searchResults.setValue(page.getList());
                            totalResults.setValue(page.getTotal());
                            hasMore.setValue(currentPage < page.getPages());
                        } else {
                            errorMessage.setValue(result.getMessage());
                        }
                    },
                    throwable -> {
                        loading.setValue(false);
                        Log.e(TAG, "搜索失败", throwable);
                        errorMessage.setValue("搜索失败：" + throwable.getMessage());
                    }
                )
        );
    }

    /** 加载更多搜索结果（分页）. */
    public void loadMore() {
        if (Boolean.FALSE.equals(hasMore.getValue()) || Boolean.TRUE.equals(loading.getValue())) {
            return;
        }
        currentPage++;
        loading.setValue(true);

        disposables.add(
            bookRepository.searchBooks(currentKeyword, null, null, null, currentPage, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        loading.setValue(false);
                        if (result.isSuccess() && result.getData() != null) {
                            PageResult<BookVO> page = result.getData();
                            List<BookVO> currentList = searchResults.getValue();
                            if (currentList != null) {
                                currentList.addAll(page.getList());
                                searchResults.setValue(currentList);
                            }
                            hasMore.setValue(currentPage < page.getPages());
                        } else {
                            currentPage--;
                            errorMessage.setValue(result.getMessage());
                        }
                    },
                    throwable -> {
                        loading.setValue(false);
                        currentPage--;
                        Log.e(TAG, "加载更多失败", throwable);
                    }
                )
        );
    }

    /** 获取搜索建议. */
    public void loadSuggestions(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            suggestions.setValue(null);
            return;
        }
        disposables.add(
            bookRepository.getSuggestions(prefix, 5)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        if (result.isSuccess() && result.getData() != null) {
                            suggestions.setValue(result.getData());
                        }
                    },
                    throwable -> Log.e(TAG, "获取搜索建议失败", throwable)
                )
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}