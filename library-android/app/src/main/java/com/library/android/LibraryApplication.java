package com.library.android;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.library.android.network.TokenManager;

import dagger.hilt.android.HiltAndroidApp;

/**
 * 图书馆智能管理系统 — Android Application 入口.
 *
 * <p>Hilt 依赖注入入口，所有 Module 由 {@code @InstallIn} 自动注册.
 * 全局初始化（网络层、日志等）由 Hilt DI Modules 统一管理.
 *
 * <p>P1-04：在 onCreate 中触发 {@link TokenManager#prewarm(android.content.Context)} 异步预热，
 * 避免冷启动时主线程首次注入 EncryptedSharedPreferences 阻塞 ANR.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@HiltAndroidApp
public class LibraryApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // D.4：全局暗色模式跟随系统（替代之前按 destination 切换的做法，统一体验）
        // 各页面如需强制特定主题（如 KG WebView），通过 ThemeManager.wrapContext 局部应用
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // P1-04：后台线程预热 TokenManager，避免 Hilt 主线程注入时阻塞主线程
        TokenManager.prewarm(this);
    }
}
