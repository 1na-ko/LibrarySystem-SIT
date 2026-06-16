package com.library.android.di;

import android.content.Context;

import androidx.room.Room;

import com.library.android.data.AppDatabase;
import com.library.android.data.dao.CachedBookDao;
import com.library.android.data.dao.CachedBorrowDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt 数据库 DI 模块（人员 B 主导）— 提供 Room Database 及 DAO 实例.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "library_cache.db"
        ).fallbackToDestructiveMigration().build();
    }

    @Provides
    @Singleton
    public CachedBookDao provideCachedBookDao(AppDatabase database) {
        return database.cachedBookDao();
    }

    @Provides
    @Singleton
    public CachedBorrowDao provideCachedBorrowDao(AppDatabase database) {
        return database.cachedBorrowDao();
    }
}
