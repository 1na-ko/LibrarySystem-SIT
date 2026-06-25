package com.library.android.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.library.android.model.BookSimpleVO;
import com.library.android.model.CategoryVO;
import com.library.android.model.PageResult;
import com.library.android.model.Result;
import com.library.android.repository.BookRepository;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 图书搜索页 ViewModel.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltViewModel
public class SearchViewModel extends BaseViewModel {

    private static final String TAG = "SearchViewModel";
    private static final int PAGE_SIZE = 20;

    private final BookRepository bookRepository;

    private final MutableLiveData<List<BookSimpleVO>> searchResults = new MutableLiveData<>();
    private final MutableLiveData<List<BookSimpleVO>> hotBooks = new MutableLiveData<>();
    private final MutableLiveData<List<CategoryVO>> categories = new MutableLiveData<>();
    /** WP-4 契约对齐：SuggestVO 含 text + type 字段，原 Map&lt;String,String&gt; 已废弃. */
    private final MutableLiveData<List<com.library.android.model.SuggestVO>> suggestions = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(true);
    private final MutableLiveData<Integer> totalResults = new MutableLiveData<>(0);
    private final MutableLiveData<String> resultTitle = new MutableLiveData<>();
    private final MutableLiveData<String> searchMethodLabel = new MutableLiveData<>();


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

    public LiveData<List<BookSimpleVO>> getSearchResults() { return searchResults; }
    public LiveData<List<BookSimpleVO>> getHotBooks() { return hotBooks; }
    public LiveData<List<CategoryVO>> getCategories() { return categories; }
    public LiveData<List<com.library.android.model.SuggestVO>> getSuggestions() { return suggestions; }
    public LiveData<Boolean> hasMore() { return hasMore; }
    public LiveData<Integer> getTotalResults() { return totalResults; }
    public LiveData<String> getResultTitle() { return resultTitle; }
    public LiveData<String> getSearchMethodLabel() { return searchMethodLabel; }

    /** 加载首页数据（热门图书 + 分类导航）. */
    public void loadHomeData() {
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
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
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        if (result.isSuccess() && result.getData() != null) {
                            categories.setValue(result.getData());
                        }
                    },
                    throwable -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        Log.e(TAG, "加载分类失败", throwable);
                    }
                )
        );
    }

    /** 按分類搜索图书. */
    public void searchByCategory(long categoryId, String categoryName) {
        currentKeyword = null;
        currentCategoryId = categoryId;
        suggestions.setValue(null);
        currentPage = 1;
        resultTitle.setValue(categoryName);
        searchMethodLabel.setValue("分类浏览");
        hasMore.setValue(true);
        setLoading(com.library.android.ui.common.LoadingState.LOADING);

        disposables.add(
            // WP-14：按分类搜索改用 advancedSearch（/books/search 后端要求 keyword 必填）
            bookRepository.advancedSearch(null, null, null, null, null, null,
                    categoryId, null, currentPage, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        if (result.isSuccess() && result.getData() != null) {
                            PageResult<BookSimpleVO> page = result.getData();
                            searchResults.setValue(page.getList());
                            totalResults.setValue((int) page.getTotal());
                            hasMore.setValue(currentPage < page.getPages());
                        } else {
                            postError(new RuntimeException(result.getMessage()));
                        }
                    },
                    throwable -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        Log.e(TAG, "分类搜索失败", throwable);
                        postError(new RuntimeException("分类搜索失败：" + throwable.getMessage()));
                    }
                )
        );
    }

    /** 搜索图书（首次搜索或切换关键词）. */
    public void search(String keyword) {
        currentKeyword = keyword;
        currentCategoryId = null;
        suggestions.setValue(null);
        resultTitle.setValue(keyword);  // 搜索结果页展示搜索词条
        searchMethodLabel.setValue("关键词搜索");
        clearAdvancedParams();
        currentPage = 1;
        hasMore.setValue(true);
        setLoading(com.library.android.ui.common.LoadingState.LOADING);

        disposables.add(
            bookRepository.searchBooks(keyword, null, null, null, currentPage, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        if (result.isSuccess() && result.getData() != null) {
                            PageResult<BookSimpleVO> page = result.getData();
                            searchResults.setValue(page.getList());
                            totalResults.setValue((int) page.getTotal());
                            hasMore.setValue(currentPage < page.getPages());
                        } else {
                            postError(new RuntimeException(result.getMessage()));
                        }
                    },
                    throwable -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        Log.e(TAG, "搜索失败", throwable);
                        postError(new RuntimeException("搜索失败：" + throwable.getMessage()));
                    }
                )
        );
    }

    /** 加载更多搜索结果（分页）. */
    public void loadMore() {
        if (Boolean.FALSE.equals(hasMore.getValue()) || getLoadingState().getValue() == com.library.android.ui.common.LoadingState.LOADING) {
            return;
        }
        currentPage++;
        setLoading(com.library.android.ui.common.LoadingState.LOADING);

        io.reactivex.rxjava3.core.Single<Result<PageResult<BookSimpleVO>>> source;
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
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        if (result.isSuccess() && result.getData() != null) {
                            PageResult<BookSimpleVO> page = result.getData();
                            List<BookSimpleVO> currentList = searchResults.getValue();
                            if (currentList != null) {
                                currentList.addAll(page.getList());
                                searchResults.setValue(currentList);
                            }
                            hasMore.setValue(currentPage < page.getPages());
                        } else {
                            currentPage--;
                            postError(new RuntimeException(result.getMessage()));
                        }
                    },
                    throwable -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
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
        suggestions.setValue(null);
        // 按参数类型区分搜索方式标签 + resultTitle
        if (isbn != null && !isbn.isEmpty()
                && (title == null || title.isEmpty())
                && (author == null || author.isEmpty())) {
            searchMethodLabel.setValue("ISBN搜索");
            resultTitle.setValue(isbn);  // ISBN 搜索时展示 ISBN 号
        } else {
            searchMethodLabel.setValue("高级搜索");
            resultTitle.setValue("高级搜索结果");
        }
        currentPage = 1;
        hasMore.setValue(true);
        setLoading(com.library.android.ui.common.LoadingState.LOADING);
        disposables.add(
            bookRepository.advancedSearch(
                    advTitle, advAuthor, advIsbn, advPublisher,
                    advPubYearFrom, advPubYearTo, null, advOnlyAvailable,
                    currentPage, PAGE_SIZE)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        if (result.isSuccess() && result.getData() != null) {
                            PageResult<BookSimpleVO> page = result.getData();
                            searchResults.setValue(page.getList());
                            totalResults.setValue((int) page.getTotal());
                            hasMore.setValue(currentPage < page.getPages());
                        } else {
                            postError(new RuntimeException(result.getMessage()));
                        }
                    },
                    throwable -> {
                        setLoading(com.library.android.ui.common.LoadingState.CONTENT);
                        Log.e(TAG, "高级搜索失败", throwable);
                        postError(new RuntimeException("高级搜索失败：" + throwable.getMessage()));
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

    /** 清除搜索状态，回到首页视图. */
    public void clearSearchState() {
        resultTitle.setValue(null);
        searchResults.setValue(null);
        suggestions.setValue(null);
        totalResults.setValue(0);
        currentKeyword = null;
        currentCategoryId = null;
        clearAdvancedParams();
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


}
