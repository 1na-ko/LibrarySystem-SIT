package com.library.android.repository;

import com.library.android.model.*;
import com.library.android.network.LibraryApi;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

/**
 * 图书检索 Repository.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class BookRepository {

    private final LibraryApi api;

    public BookRepository(LibraryApi api) {
        this.api = api;
    }

    public Single<Result<PageResult<BookVO>>> searchBooks(String keyword, String author, Long categoryId,
                                                           String sortBy, int pageNum, int pageSize) {
        return Single.fromCallable(() ->
                api.searchBooks(keyword, author, categoryId, sortBy, pageNum, pageSize).execute().body());
    }

    public Single<Result<BookDetailVO>> getBookDetail(long bookId) {
        return Single.fromCallable(() -> api.getBookDetail(bookId).execute().body());
    }

    public Single<Result<List<BookRecommendVO>>> getRelatedBooks(long bookId, int limit) {
        return Single.fromCallable(() -> api.getRelatedBooks(bookId, limit).execute().body());
    }

    public Single<Result<List<BookVO>>> getHotBooks(Long categoryId, int limit) {
        return Single.fromCallable(() -> api.hotBooks(categoryId, limit).execute().body());
    }

    public Single<Result<List<CategoryVO>>> getCategoryTree() {
        return Single.fromCallable(() -> api.getCategoryTree().execute().body());
    }

    public Single<Result<List<Map<String, String>>>> getSuggestions(String prefix, int limit) {
        return Single.fromCallable(() -> api.suggest(prefix, limit).execute().body());
    }

    public Single<Result<PageResult<BookVO>>> advancedSearch(
            String title, String author, String isbn, String publisher,
            Integer pubYearFrom, Integer pubYearTo, Long categoryId,
            Boolean onlyAvailable, int pageNum, int pageSize) {
        return Single.fromCallable(() -> api.advancedSearch(
                title, author, isbn, publisher, pubYearFrom, pubYearTo,
                categoryId, onlyAvailable, pageNum, pageSize).execute().body());
    }
}