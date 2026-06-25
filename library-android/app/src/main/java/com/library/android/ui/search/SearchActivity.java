package com.library.android.ui.search;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.library.android.R;
import com.library.android.databinding.ActivitySearchBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 独立搜索 Activity（WP-14）：真正的无主 header/无 BottomNav 独立页面.
 *
 * <p>由 MainActivity 全局搜索按钮启动，承载 SearchFragment → SearchResultsFragment → BookDetail 完整搜索流.
 * 按系统返回键逐级回退，SearchFragment 栈底时 finish() 回到 MainActivity.
 */
@AndroidEntryPoint
public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.search_nav_host);
        navController = navHost.getNavController();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if (!navController.navigateUp()) {
            super.onBackPressed();
        }
    }
}
