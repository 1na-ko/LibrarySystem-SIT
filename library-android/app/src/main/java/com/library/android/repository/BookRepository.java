package com.library.android.repository;

import com.library.android.model.*;
import com.library.android.network.ApiCallExecutor;
import com.library.android.network.LibraryApi;

import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.core.Single;

/**
 * 图书检索 Repository.
 *
 * <p>所有方法通过 {@link ApiCallExecutor} 执行，HTTP 错误码会翻译成
 * {@link com.library.android.network.exception.ApiException} 及子类，
 * 由 ViewModel 在 {@code onError} 中按类型处理.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class BookRepository {

    private final LibraryApi api;

    public BookRepository(LibraryApi api) {
        this.api = api;
    }

    public Single<Result<PageResult<BookSimpleVO>>> searchBooks(String keyword, String author, Long categoryId,
                                                                  String sortBy, int pageNum, int pageSize) {
        return Single.fromCallable(() -> ApiCallExecutor.execute(
                api.searchBooks(keyword, author, categoryId, sortBy, pageNum, pageSize)));
    }

    public Single<Result<BookDetailVO>> getBookDetail(long bookId) {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.getBookDetail(bookId)));
    }

    public Single<Result<List<BookRecommendVO>>> getRelatedBooks(long bookId, int limit) {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.getRelatedBooks(bookId, limit)));
    }

    public Single<Result<List<BookSimpleVO>>> getHotBooks(Long categoryId, int limit) {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.hotBooks(categoryId, limit)));
    }

    public Single<Result<List<CategoryVO>>> getCategoryTree() {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.getCategoryTree()));
    }

    /** WP2.8：按父分类列出子分类（补充此前遗漏的 Repository 封装）. */
    public Single<Result<List<CategoryVO>>> listCategories(Long parentId) {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.listCategories(parentId)));
    }

    /** WP2.8：获取单个分类详情. */
    public Single<Result<CategoryVO>> getCategory(long categoryId) {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.getCategory(categoryId)));
    }

    /** WP-4 契约对齐：返回 SuggestVO（含 text/type 字段，原 Map&lt;String,String&gt; 已废弃）. */
    public Single<Result<List<com.library.android.model.SuggestVO>>> getSuggestions(String prefix, int limit) {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.suggest(prefix, limit)));
    }

    public Single<Result<PageResult<BookSimpleVO>>> advancedSearch(
            String title, String author, String isbn, String publisher,
            Integer pubYearFrom, Integer pubYearTo, Long categoryId,
            Boolean onlyAvailable, int pageNum, int pageSize) {
        return Single.fromCallable(() -> ApiCallExecutor.execute(api.advancedSearch(
                title, author, isbn, publisher, pubYearFrom, pubYearTo,
                categoryId, onlyAvailable, pageNum, pageSize)));
    }
}
