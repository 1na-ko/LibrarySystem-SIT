package com.library.android;

import android.app.Application;

/**
 * 图书馆智能管理系统 — Android Application 入口.
 *
 * <p>负责全局初始化：Hilt 依赖注入、日志配置、全局异常处理.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class LibraryApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // Hilt 自动注入，此处预留给全局初始化逻辑
    }
}
