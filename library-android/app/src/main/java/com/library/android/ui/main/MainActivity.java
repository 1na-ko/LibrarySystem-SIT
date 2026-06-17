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
import com.library.android.network.TokenManager;
import com.library.android.ui.search.SearchFragment;
import com.library.android.ui.theme.ThemeManager;

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

    private ActivityMainBinding binding;
    private NavController navController;

    private static final Set<Integer> DARK_MODE_DESTINATIONS = new HashSet<>(Arrays.asList(
            R.id.knowledgeGraphFragment,
            R.id.literatureTraceFragment,
            R.id.subjectNetworkFragment,
            R.id.entitySearchFragment,
            R.id.adminUserListFragment
    ));

    /** 登录/注册页面 */
    private static final Set<Integer> CLEAN_SCREEN_DESTINATIONS = new HashSet<>(Arrays.asList(
            R.id.loginFragment,
            R.id.registerFragment
    ));

    /** 主 Tab 页面（返回按钮隐藏） */
    private static final Set<Integer> TOP_LEVEL_DESTINATIONS = new HashSet<>(Arrays.asList(
            R.id.searchFragment,
            R.id.borrowFragment,
            R.id.profileFragment
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

        // 全局返回按钮
        binding.btnGlobalBack.setOnClickListener(v -> onBackPressed());

        // 全局搜索图标：切换到搜索 Tab 并展开搜索栏
        binding.btnGlobalSearch.setOnClickListener(v -> {
            if (binding.bottomNavigation.getSelectedItemId() != R.id.searchFragment) {
                binding.bottomNavigation.setSelectedItemId(R.id.searchFragment);
            }
            triggerSearchExpand();
        });

        // 目的地变化监听
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();

            if (CLEAN_SCREEN_DESTINATIONS.contains(destId)) {
                // 登录/注册：隐藏全局头部 + 底部导航
                binding.layoutGlobalHeader.setVisibility(View.GONE);
                binding.bottomNavigation.setVisibility(View.GONE);
                binding.dividerBottomNav.setVisibility(View.GONE);
            } else {
                binding.layoutGlobalHeader.setVisibility(View.VISIBLE);
                binding.bottomNavigation.setVisibility(View.VISIBLE);
                binding.dividerBottomNav.setVisibility(View.VISIBLE);

                // 返回按钮：主 Tab 隐藏，子页面显示
                binding.btnGlobalBack.setVisibility(
                        TOP_LEVEL_DESTINATIONS.contains(destId) ? View.GONE : View.VISIBLE);

                // 主 Tab 页面恢复默认标题
                if (TOP_LEVEL_DESTINATIONS.contains(destId)) {
                    setGlobalTitle("图书馆");
                }
            }

            // 深色模式
            ThemeManager.getInstance().setDarkMode(
                    DARK_MODE_DESTINATIONS.contains(destId));
        });

        if (!tokenManager.isLoggedIn()) {
            navController.navigate(R.id.loginFragment);
        }
    }

    private void triggerSearchExpand() {
        binding.btnGlobalSearch.postDelayed(() -> {
            Fragment navChild = getCurrentNavFragment();
            if (navChild instanceof SearchFragment) {
                ((SearchFragment) navChild).expandSearchFromGlobal();
            }
        }, 200);
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
