package com.library.android.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.library.android.data.dao.CachedBookDao;
import com.library.android.data.dao.CachedBorrowDao;
import com.library.android.data.entity.CachedBookEntity;
import com.library.android.data.entity.CachedBorrowEntity;

/**
 * Room 数据库 — 本地缓存层（离线降级）.
 *
 * <p>表设计：仅缓存最近查看的图书列表与当前用户的借阅记录快照。
 * 不作为主数据源，网络请求始终优先。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Database(
        entities = {CachedBookEntity.class, CachedBorrowEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract CachedBookDao cachedBookDao();
    public abstract CachedBorrowDao cachedBorrowDao();
}
