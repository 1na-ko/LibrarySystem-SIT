package com.library.android;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

/**
 * 图书馆智能管理系统 — Android Application 入口.
 *
 * <p>Hilt 依赖注入入口，所有 Module 由 {@code @InstallIn} 自动注册.
 * 全局初始化（网络层、日志等）由 Hilt DI Modules 统一管理.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltAndroidApp
public class LibraryApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }
}