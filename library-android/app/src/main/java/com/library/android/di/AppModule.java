package com.library.android.di;

import android.content.Context;

import com.library.android.LibraryApplication;
import com.library.android.util.TokenManager;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

/**
 * 应用级依赖提供模块.
 *
 * <p>提供全局单例依赖：ApplicationContext、TokenManager 等.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    static TokenManager provideTokenManager(@ApplicationContext Context context) {
        return TokenManager.getInstance(context);
    }
}