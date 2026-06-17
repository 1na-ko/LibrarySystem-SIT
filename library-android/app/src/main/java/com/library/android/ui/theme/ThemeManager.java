package com.library.android.ui.theme;

import android.content.Context;
import android.view.ContextThemeWrapper;

import com.library.android.R;

/**
 * Design Tokens v2.1 — 深色模式管理器
 *
 * 管理知识图谱和采编模块的深色主题切换。
 * 不使用 Android 系统 DayNight 自动模式 —— 仅在指定页面应用深色主题。
 *
 * 用法：
 *   // Fragment 中使用：
 *   Context themed = ThemeManager.getInstance().wrapContext(requireContext());
 *   LayoutInflater themedInflater = inflater.cloneInContext(themed);
 *   binding = FragmentXxxBinding.inflate(themedInflater, container, false);
 */
public class ThemeManager {

    private static ThemeManager instance;

    private boolean darkMode = false;

    private ThemeManager() {
    }

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    /**
     * 当前是否处于深色模式。
     */
    public boolean isDarkMode() {
        return darkMode;
    }

    /**
     * 设置深色模式状态。应在 Activity 的 Navigation 目的地监听中调用。
     */
    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
    }

    /**
     * 返回一个带正确主题的 Context，用于 LayoutInflater 加载布局。
     *
     * @param context 原始 Context（通常是 requireContext() 或 this）
     * @return 包装了深色或浅色主题的 ContextThemeWrapper
     */
    public Context wrapContext(Context context) {
        int themeRes = darkMode
                ? R.style.Theme_LibrarySystem_Dark
                : R.style.Theme_LibrarySystem;
        return new ContextThemeWrapper(context, themeRes);
    }
}
