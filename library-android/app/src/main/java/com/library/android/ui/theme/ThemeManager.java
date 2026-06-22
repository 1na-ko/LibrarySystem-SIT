package com.library.android.ui.theme;

import android.content.Context;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import com.library.android.R;

import java.util.Collections;
import java.util.Set;

/**
 * Design Tokens v2.1 — 深色模式管理器
 *
 * <p>管理知识图谱、采编、管理模块的强制深色主题切换。
 * 这些页面不使用 Android 系统 DayNight 自动模式，始终渲染为深色；
 * 其他页面跟随系统 DayNight 设置。
 *
 * <p>用法：
 * <pre>{@code
 *   // 强制深色 Fragment 的 onCreateView 中：
 *   ThemeManager.getInstance().setDarkMode(true);
 *   Context themed = ThemeManager.getInstance().wrapContext(requireContext());
 *   LayoutInflater themedInflater = inflater.cloneInContext(themed);
 *   binding = FragmentXxxBinding.inflate(themedInflater, container, false);
 * }</pre>
 */
public class ThemeManager {

    private static ThemeManager instance;

    /**
     * 需要强制深色的 Navigation 目的地 ID。
     * 与 {@link #setDarkMode(boolean)} 配合使用，供 Activity 级导航监听统一维护状态。
     */
    private static final Set<Integer> DARK_DESTINATIONS;

    static {
        Set<Integer> set = new ArraySet<>();
        // WP-FE-FIX：知识图谱 4 个 Fragment 已改为跟随应用 DayNight，不再强制深色
        // set.add(R.id.knowledgeGraphFragment);
        // set.add(R.id.literatureTraceFragment);
        // set.add(R.id.subjectNetworkFragment);
        // set.add(R.id.entitySearchFragment);
        // 智能采编
        set.add(R.id.acquisitionFragment);
        set.add(R.id.purchasePredictFragment);
        set.add(R.id.duplicateCheckFragment);
        set.add(R.id.gapAnalysisFragment);
        set.add(R.id.negotiationCreateFragment);
        set.add(R.id.negotiationDetailFragment);
        // 系统管理
        set.add(R.id.adminDashboardFragment);
        set.add(R.id.adminUserListFragment);
        DARK_DESTINATIONS = Collections.unmodifiableSet(set);
    }

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
     * 设置深色模式状态。
     * 应在 Activity 的 Navigation 目的地监听中调用，用于同步全局状态；
     * 强制深色 Fragment 还需在 {@code onCreateView} 中再次设置，
     * 确保从返回栈恢复或配置变更时 inflate 前状态正确。
     */
    public void setDarkMode(boolean dark) {
        this.darkMode = dark;
    }

    /**
     * 判断指定 Navigation 目的地是否属于强制深色页面。
     */
    public boolean isDarkDestination(int destinationId) {
        return DARK_DESTINATIONS.contains(destinationId);
    }

    /**
     * 返回一个带正确主题的 Context，用于 LayoutInflater 加载布局。
     *
     * @param context 原始 Context（通常是 requireContext() 或 this）
     * @return 包装了深色或浅色主题的 ContextThemeWrapper
     */
    public Context wrapContext(@NonNull Context context) {
        int themeRes = darkMode
                ? R.style.Theme_LibrarySystem_Dark
                : R.style.Theme_LibrarySystem;
        return new ContextThemeWrapper(context, themeRes);
    }
}
