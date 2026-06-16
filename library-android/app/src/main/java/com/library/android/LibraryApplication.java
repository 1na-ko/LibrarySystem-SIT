package com.library.android;

import android.app.Application;

import com.library.android.network.RetrofitClient;

import dagger.hilt.android.HiltAndroidApp;

/**
 * 图书馆智能管理系统 — Android Application 入口.
 *
 * <p>负责全局初始化：Hilt 依赖注入、日志配置、全局异常处理.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltAndroidApp
public class LibraryApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        RetrofitClient.init(this);
    }
}