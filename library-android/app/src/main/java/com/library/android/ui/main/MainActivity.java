package com.library.android.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.library.android.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 主 Activity — 应用入口页面.
 *
 * <p>包含底部导航栏，承载搜索、借阅、个人中心等 Fragment。
 * 使用 Hilt 进行依赖注入。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
