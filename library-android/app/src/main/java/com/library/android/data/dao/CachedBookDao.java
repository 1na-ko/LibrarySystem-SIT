package com.library.android.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.library.android.data.entity.CachedBookEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * 缓存图书 DAO.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Dao
public interface CachedBookDao {

    @Query("SELECT * FROM cached_books ORDER BY cached_at DESC LIMIT :limit")
    Single<List<CachedBookEntity>> getRecentBooks(int limit);

    @Query("SELECT * FROM cached_books WHERE id = :bookId")
    Maybe<CachedBookEntity> getBookById(long bookId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertOrUpdate(CachedBookEntity book);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertOrUpdateAll(List<CachedBookEntity> books);

    @Query("DELETE FROM cached_books WHERE cached_at < :threshold")
    Completable deleteStale(long threshold);
}
