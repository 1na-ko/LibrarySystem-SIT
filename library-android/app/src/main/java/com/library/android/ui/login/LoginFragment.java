package com.library.android.ui.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.library.android.R;
import com.library.android.databinding.FragmentLoginBinding;
import com.library.android.viewmodel.LoginViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 登录页面 Fragment.
 *
 * <p>使用 Hilt @AndroidEntryPoint，LoginViewModel 由 Hilt 自动注入依赖.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";
    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        viewModel.isLoginSuccess().observe(getViewLifecycleOwner(), success -> {
            Log.d(TAG, "登录结果: success=" + success);
            if (success) {
                NavOptions navOptions = new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .setLaunchSingleTop(true)
                        .build();
                Navigation.findNavController(view)
                        .navigate(R.id.homeFragment, null, navOptions);
            }
        });

        viewModel.getErrorEvent().observe(getViewLifecycleOwner(), throwable -> {
            Log.d(TAG, "错误消息: " + (throwable != null ? throwable.getMessage() : ""));
            if (throwable != null && throwable.getMessage() != null && !throwable.getMessage().isEmpty()) {
                Toast.makeText(requireContext(), throwable.getMessage(), Toast.LENGTH_LONG).show();
                binding.etUsername.setError(throwable.getMessage());
            }
        });

        viewModel.getLoadingState().observe(getViewLifecycleOwner(), state -> {
            Log.d(TAG, "加载状态: " + state);
            boolean isLoading = state == com.library.android.ui.common.LoadingState.LOADING;
            binding.btnLogin.setEnabled(!isLoading);
            binding.btnRegister.setEnabled(!isLoading);
        });

        binding.btnLogin.setOnClickListener(v -> {
            // ── 按钮光泽扫过动效 ──
            triggerShineAnimation();

            String username = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            // 移除 PII 日志：原代码会打印 username 与"已填写"至 logcat（OWASP 违规）
            Log.d(TAG, "点击登录");
            // ViewModel 已通过 Hilt 注入 TokenManager，无需传 Context
            viewModel.login(username, password);
        });

        binding.btnRegister.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_loginFragment_to_registerFragment));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * 登录按钮光泽扫过动效：从左侧滑入，经按钮表面扫至右侧消失。
     *
     * <p>光泽条纹使用 {@code viewShineOverlay}（FrameLayout 内叠加在按钮上方），
     * 通过 ObjectAnimator 平移 translationX 实现对角线高光扫过效果，
     * 持续 800ms，加速-减速插值器模拟自然光泽。
     */
    private void triggerShineAnimation() {
        final View shineOverlay = binding.viewShineOverlay;
        final int shineWidth = shineOverlay.getLayoutParams().width;
        if (shineWidth <= 0) {
            return; // 布局尚未测量，跳过动画
        }

        shineOverlay.setVisibility(View.VISIBLE);
        shineOverlay.setTranslationX(-shineWidth); // 从按钮左侧外开始

        ObjectAnimator animator = ObjectAnimator.ofFloat(
                shineOverlay, "translationX",
                -shineWidth,           // from: 左侧外
                shineWidth * 1.5f      // to:   右侧外
        );
        animator.setDuration(800);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                shineOverlay.setVisibility(View.INVISIBLE);
            }
        });
        animator.start();
    }
}
