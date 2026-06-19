package com.library.android.ui.main;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.library.android.R;
import com.library.android.databinding.ActivityMainBinding;
import com.library.android.network.SessionManager;
import com.library.android.network.TokenManager;
import com.library.android.ui.search.SearchFragment;

import dagger.hilt.android.AndroidEntryPoint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

/**
 * 主 Activity — 应用入口页面.
 *
 * <p>全局顶部栏：返回按钮（左）+ 标题居中 + 搜索图标（右），
 * 在所有主 tab 页面均可见。登录/注册页隐藏全部.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject
    TokenManager tokenManager;

    @Inject
    SessionManager sessionManager;

    private ActivityMainBinding binding;
    private NavController navController;

    // WP-14：深色模式已改为全局 DayNight（跟随系统），不再按 destination 选择性应用

    /** 登录/注册页面（全屏，无头部无 Tab） */
    private static final Set<Integer> CLEAN_SCREEN_DESTINATIONS = new HashSet<>(Arrays.asList(
            R.id.loginFragment,
            R.id.registerFragment
    ));

    /** Tab 根页面（首页/借阅/我的）— 显示 Activity header + 底部 Tab，无返回键 */
    private static final Set<Integer> TAB_ROOT_DESTINATIONS = new HashSet<>(Arrays.asList(
            R.id.homeFragment,
            R.id.borrowFragment,
            R.id.profileFragment
    ));

    /**
     * Tab 根页面中右上角显示全局搜索按钮的页面（WP-1：按页定制）.
     * <p>仅首页有搜索按钮，借阅/我的页不应常驻搜索按钮——它们的右上角按钮由各 Fragment 自行接管.
     */
    private static final Set<Integer> SHOW_GLOBAL_SEARCH_DESTINATIONS = new HashSet<>(java.util.Collections.singletonList(
            R.id.homeFragment
    ));

    /**
     * 已接入页面级 page_toolbar 的二级页面（WP-1 渐进式迁移）.
     * <p>这些页面在 Activity header 上完全不显示（由 page_toolbar 接管返回+标题+操作按钮）；
     * 其他二级页面继续暂用 Activity header 兼容（搜索按钮强制隐藏，仅显示返回+标题），
     * 在各业务 WP 推进时逐页迁入此 Set + layout 接 include page_toolbar.
     */
    /** WP-14：已接入页面级 page_toolbar 的二级页面（隐藏 Activity header 避免双 header）. */
    private static final Set<Integer> PAGE_TOOLBAR_DESTINATIONS = new HashSet<>(java.util.Arrays.asList(
            R.id.searchResultsFragment
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHost.getNavController();
        NavigationUI.setupWithNavController(
                binding.bottomNavigation, navController);

        // WP-1：全局返回按钮已不再使用（二级页面由 page_toolbar 接管返回），保留点击以防意外可见
        binding.btnGlobalBack.setOnClickListener(v -> onBackPressed());

        // WP-14：全局搜索按钮 → 启动独立 SearchActivity（真正无主 header/无 BottomNav）
        binding.btnGlobalSearch.setOnClickListener(v ->
                startActivity(new android.content.Intent(this,
                        com.library.android.ui.search.SearchActivity.class)));

        // WP-1 治本：目的地变化监听 — 三类页面区分
        // ① CLEAN_SCREEN：登录/注册全屏（无 header / 无 Tab）
        // ② TAB_ROOT：Tab 根页（显示 Activity header + Tab，header 内搜索按钮按页定制）
        // ③ 二级页面：隐藏 Activity header（由页面级 page_toolbar 接管）+ 隐藏 Tab
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();

            if (CLEAN_SCREEN_DESTINATIONS.contains(destId)) {
                // ① 登录/注册：全屏
                binding.layoutGlobalHeader.setVisibility(View.GONE);
                binding.bottomNavigation.setVisibility(View.GONE);
                binding.dividerBottomNav.setVisibility(View.GONE);
            } else if (TAB_ROOT_DESTINATIONS.contains(destId)) {
                // ② Tab 根页：Activity header + Tab；搜索按钮按页定制
                binding.layoutGlobalHeader.setVisibility(View.VISIBLE);
                binding.bottomNavigation.setVisibility(View.VISIBLE);
                binding.dividerBottomNav.setVisibility(View.VISIBLE);
                binding.btnGlobalBack.setVisibility(View.GONE);
                binding.btnGlobalSearch.setVisibility(
                        SHOW_GLOBAL_SEARCH_DESTINATIONS.contains(destId) ? View.VISIBLE : View.GONE);
                // 默认标题（页面 onViewCreated 中可调 setGlobalTitle 覆盖）
                if (destId == R.id.homeFragment) {
                    setGlobalTitle(getString(R.string.home_title));
                } else if (destId == R.id.borrowFragment) {
                    setGlobalTitle(getString(R.string.nav_borrow));
                } else if (destId == R.id.profileFragment) {
                    setGlobalTitle(getString(R.string.nav_profile));
                }
            } else {
                // ③ 二级页面：渐进式迁移
                //    若已迁入 PAGE_TOOLBAR_DESTINATIONS（layout 含 page_toolbar）→ 完全隐藏 Activity header
                //    否则暂用 Activity header（兼容路径：返回按钮 + 标题，但隐藏搜索按钮）
                boolean migrated = PAGE_TOOLBAR_DESTINATIONS.contains(destId);
                binding.bottomNavigation.setVisibility(View.GONE);
                binding.dividerBottomNav.setVisibility(View.GONE);
                if (migrated) {
                    binding.layoutGlobalHeader.setVisibility(View.GONE);
                } else {
                    binding.layoutGlobalHeader.setVisibility(View.VISIBLE);
                    binding.btnGlobalBack.setVisibility(View.VISIBLE);
                    binding.btnGlobalSearch.setVisibility(View.GONE); // WP-1：二级页不常驻搜索按钮
                }
            }
        });

        if (!tokenManager.isLoggedIn()) {
            navController.navigate(R.id.loginFragment);
        }

        // 监听全局会话失效事件（TokenAuthenticator refresh 失败时触发）
        // → 强制跳回登录页并清栈，避免用户卡死在错误页
        sessionManager.getSessionExpired().observe(this, expired -> {
            if (Boolean.TRUE.equals(expired) && navController.getCurrentDestination() != null
                    && navController.getCurrentDestination().getId() != R.id.loginFragment) {
                androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .setLaunchSingleTop(true)
                        .build();
                navController.navigate(R.id.loginFragment, null, navOptions);
                sessionManager.consume();
                android.widget.Toast.makeText(this,
                        getString(R.string.session_expired_toast),
                        android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    private Fragment getCurrentNavFragment() {
        Fragment navHost = getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHost instanceof NavHostFragment) {
            return ((NavHostFragment) navHost)
                    .getChildFragmentManager().getPrimaryNavigationFragment();
        }
        return null;
    }

    /**
     * 供 SearchFragment 控制全局返回按钮可见性.
     * 搜索结果/分类浏览页中显示，主界面隐藏.
     */
    public void setGlobalBackVisible(boolean visible) {
        if (binding != null) {
            binding.btnGlobalBack.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * 设置全局顶部栏标题.
     * 子页面通过此方法将标题同步到全局 header.
     */
    public void setGlobalTitle(String title) {
        if (binding != null) {
            binding.tvGlobalTitle.setText(title);
        }
    }

    @Override
    public void onBackPressed() {
        // 优先让 SearchFragment 处理内部返回
        Fragment navChild = getCurrentNavFragment();
        if (navChild instanceof SearchFragment) {
            if (((SearchFragment) navChild).onBackPressed()) {
                return;
            }
        }
        // NavController 处理返回
        if (!navController.navigateUp()) {
            super.onBackPressed();
        }
    }
}
