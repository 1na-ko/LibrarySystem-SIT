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
    private Long currentCategoryId;
    private int currentPage = 1;

    // 高级搜索参数
    private String advTitle, advAuthor, advIsbn, advPublisher;
    private Integer advPubYearFrom, advPubYearTo;
    private Boolean advOnlyAvailable;

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

    /** 按分類搜索图书. */
    public void searchByCategory(long categoryId, String categoryName) {
        currentKeyword = null;
        currentCategoryId = categoryId;
        currentPage = 1;
        hasMore.setValue(true);
        loading.setValue(true);

        disposables.add(
            bookRepository.searchBooks(null, null, categoryId, null, currentPage, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        loading.setValue(false);
                        if (result.isSuccess() && result.getData() != null) {
                            PageResult<BookVO> page = result.getData();
                            searchResults.setValue(page.getList());
                            totalResults.setValue((int) page.getTotal());
                            hasMore.setValue(currentPage < page.getPages());
                        } else {
                            errorMessage.setValue(result.getMessage());
                        }
                    },
                    throwable -> {
                        loading.setValue(false);
                        Log.e(TAG, "分类搜索失败", throwable);
                        errorMessage.setValue("分类搜索失败：" + throwable.getMessage());
                    }
                )
        );
    }

    /** 搜索图书（首次搜索或切换关键词）. */
    public void search(String keyword) {
        currentKeyword = keyword;
        currentCategoryId = null;
        clearAdvancedParams();
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
                            totalResults.setValue((int) page.getTotal());
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

        io.reactivex.rxjava3.core.Single<Result<PageResult<BookVO>>> source;
        if (isAdvancedMode()) {
            source = bookRepository.advancedSearch(
                    advTitle, advAuthor, advIsbn, advPublisher,
                    advPubYearFrom, advPubYearTo, null, advOnlyAvailable,
                    currentPage, PAGE_SIZE);
        } else {
            source = bookRepository.searchBooks(currentKeyword, null, currentCategoryId, null, currentPage, PAGE_SIZE);
        }

        disposables.add(
            source
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

    /** 高级搜索. */
    public void searchAdvanced(String title, String author, String isbn, String publisher,
                               Integer pubYearFrom, Integer pubYearTo, Boolean onlyAvailable) {
        currentKeyword = null;
        currentCategoryId = null;
        this.advTitle = title;
        this.advAuthor = author;
        this.advIsbn = isbn;
        this.advPublisher = publisher;
        this.advPubYearFrom = pubYearFrom;
        this.advPubYearTo = pubYearTo;
        this.advOnlyAvailable = onlyAvailable;
        currentPage = 1;
        hasMore.setValue(true);
        loading.setValue(true);

        disposables.add(
            bookRepository.advancedSearch(
                    advTitle, advAuthor, advIsbn, advPublisher,
                    advPubYearFrom, advPubYearTo, null, advOnlyAvailable,
                    currentPage, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        loading.setValue(false);
                        if (result.isSuccess() && result.getData() != null) {
                            PageResult<BookVO> page = result.getData();
                            searchResults.setValue(page.getList());
                            totalResults.setValue((int) page.getTotal());
                            hasMore.setValue(currentPage < page.getPages());
                        } else {
                            errorMessage.setValue(result.getMessage());
                        }
                    },
                    throwable -> {
                        loading.setValue(false);
                        Log.e(TAG, "高级搜索失败", throwable);
                        errorMessage.setValue("高级搜索失败：" + throwable.getMessage());
                    }
                )
        );
    }

    private void clearAdvancedParams() {
        advTitle = null;
        advAuthor = null;
        advIsbn = null;
        advPublisher = null;
        advPubYearFrom = null;
        advPubYearTo = null;
        advOnlyAvailable = null;
    }

    /** 判断当前是否为高级搜索模式. */
    private boolean isAdvancedMode() {
        return advTitle != null || advAuthor != null || advIsbn != null
                || advPublisher != null || advPubYearFrom != null
                || advPubYearTo != null || advOnlyAvailable != null;
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