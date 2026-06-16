package com.library.android.ui.main;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.library.android.R;
import com.library.android.databinding.ActivityMainBinding;
import com.library.android.util.TokenManager;

/**
 * 主 Activity — 应用入口页面.
 *
 * <p>包含底部导航栏，承载搜索、借阅、个人中心等 Fragment.
 * 启动时检查登录状态，未登录则跳转登录页.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHost.getNavController();
        NavigationUI.setupWithNavController(
                binding.bottomNavigation, navController);

        // 登录页面隐藏底部导航栏
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.loginFragment
                    || destination.getId() == R.id.registerFragment) {
                binding.bottomNavigation.setVisibility(View.GONE);
            } else {
                binding.bottomNavigation.setVisibility(View.VISIBLE);
            }
        });

        if (!TokenManager.getInstance(this).isLoggedIn()) {
            navController.navigate(R.id.loginFragment);
        }
    }
}