package com.library.android.ui.main;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.library.android.R;
import com.library.android.databinding.ActivityMainBinding;
import com.library.android.network.TokenManager;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

/**
 * 主 Activity — 应用入口页面.
 *
 * <p>包含底部导航栏，承载搜索、借阅、个人中心等 Fragment.
 * 启动时检查登录状态，未登录则跳转登录页.
 * 使用 Hilt @AndroidEntryPoint 支持依赖注入.
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

        // 登录/注册页面隐藏底部导航栏
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.loginFragment
                    || destination.getId() == R.id.registerFragment) {
                binding.bottomNavigation.setVisibility(View.GONE);
            } else {
                binding.bottomNavigation.setVisibility(View.VISIBLE);
            }
        });

        if (!tokenManager.isLoggedIn()) {
            navController.navigate(R.id.loginFragment);
        }
    }

    @Override
    public void onBackPressed() {
        // 优先由 NavController 处理返回，当前已是最顶层时才退出
        if (!navController.navigateUp()) {
            super.onBackPressed();
        }
    }
}