package com.library.android;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

/**
 * 图书馆智能管理系统 — Android Application 入口.
 *
 * <p>使用 Hilt 进行依赖注入。全局初始化逻辑在此处集中管理。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltAndroidApp
public class LibraryApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Hilt 自动注入，全局初始化逻辑在此处追加
    }
}
