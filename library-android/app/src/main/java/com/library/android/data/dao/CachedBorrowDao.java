package com.library.android.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.library.android.data.entity.CachedBorrowEntity;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;

/**
 * 缓存借阅记录 DAO.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Dao
public interface CachedBorrowDao {

    @Query("SELECT * FROM cached_borrows WHERE user_id = :userId AND status = :status ORDER BY cached_at DESC")
    Single<List<CachedBorrowEntity>> getByUserAndStatus(long userId, String status);

    @Query("SELECT * FROM cached_borrows WHERE user_id = :userId ORDER BY cached_at DESC LIMIT :limit")
    Single<List<CachedBorrowEntity>> getByUser(long userId, int limit);

    @Query("SELECT * FROM cached_borrows WHERE id = :borrowId")
    Maybe<CachedBorrowEntity> getById(long borrowId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    Completable insertOrUpdateAll(List<CachedBorrowEntity> borrows);

    @Query("DELETE FROM cached_borrows WHERE id = :borrowId")
    Completable deleteById(long borrowId);

    @Query("DELETE FROM cached_borrows WHERE cached_at < :threshold")
    Completable deleteStale(long threshold);
}
