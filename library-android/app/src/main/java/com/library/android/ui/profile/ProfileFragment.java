package com.library.android.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.library.android.R;
import com.library.android.databinding.FragmentProfileBinding;
import com.library.android.model.BorrowStatsVO;
import com.library.android.model.UserProfile;
import com.library.android.network.TokenManager;
import com.library.android.viewmodel.ProfileViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 个人中心 Fragment — 含用户信息卡片、借阅概览、功能入口.
 *
 * <p>P1-01：移除 {@code @Inject AuthRepository}，登出动作下沉到
 * {@link ProfileViewModel#logout()}，UI 监听 logoutCompleted SingleLiveEvent
 * 后执行本地清退 + 跳登录.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // B.4 关键修复：使用 requireActivity() scope 与 EditProfileFragment / BorrowStatsFragment 保持一致
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // 立即同步设置初始 UI，避免 XML 默认值导致"未登录"闪现
        updateUI(viewModel.getUserProfile().getValue());

        // 登录按钮 — WP-1：加 NavOptions 防栈叠加 + launchSingleTop
        binding.btnLogin.setOnClickListener(v -> {
            NavOptions navOptions = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .build();
            Navigation.findNavController(view).navigate(R.id.loginFragment, null, navOptions);
        });

        // P1-01：退出登录由 ViewModel 统一处理，UI 仅触发 + 监听完成事件
        binding.btnLogout.setOnClickListener(v -> viewModel.logout());

        // 功能入口点击
        binding.layoutBorrowHistory.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_borrowHistoryFragment));

        binding.layoutBorrowStats.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_borrowStatsFragment));

        // WP1.4：推荐入口已移至首页，layoutRecommendations 已从布局中删除
        binding.layoutEditProfile.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_editProfileFragment));

        binding.layoutReservations.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_reservationListFragment));

        // C.4 新增：管理员/采编员区块入口（按角色显示）
        binding.layoutDashboard.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_adminDashboardFragment));
        binding.layoutAdminUsers.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_adminUserListFragment));
        binding.layoutOverdueMgmt.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_overdueFragment));
        binding.layoutAcquisition.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_acquisitionFragment));

        // 监听用户数据
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), this::updateUI);

        // 监听借阅统计，填充概览数字
        viewModel.getBorrowStats().observe(getViewLifecycleOwner(), this::updateBorrowOverview);

        // P1-01：登出状态切换（按钮禁用防抖）
        viewModel.isLoggingOut().observe(getViewLifecycleOwner(), inFlight -> {
            if (binding != null) {
                binding.btnLogout.setEnabled(!Boolean.TRUE.equals(inFlight));
            }
        });

        // P1-01：登出完成后做本地清退 + 跳登录
        viewModel.getLogoutCompleted().observe(getViewLifecycleOwner(), o -> performLocalLogout(view));
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次回到此页面检查登录状态
        TokenManager tm = TokenManager.getInstance(requireContext());
        if (tm.isLoggedIn() && viewModel.getUserProfile().getValue() == null) {
            viewModel.loadProfile();
            viewModel.loadBorrowStats();
        } else if (viewModel.getBorrowStats().getValue() == null && tm.isLoggedIn()) {
            viewModel.loadBorrowStats();
        }
        if (!tm.isLoggedIn()) {
            updateUI(null);
        } else {
            updateUI(viewModel.getUserProfile().getValue());
        }
    }

    private void updateUI(UserProfile profile) {
        TokenManager tm = TokenManager.getInstance(requireContext());
        boolean loggedIn = tm.isLoggedIn();

        // P2-05：头像占位图品牌化 — 优先显示姓名/用户名首字母，否则显示默认图标
        updateAvatar(loggedIn, profile);

        // C.4 新增：根据用户角色控制管理工具区块可见性
        if (loggedIn && tm.isLibrarianOrAbove()) {
            binding.layoutAdminSection.setVisibility(View.VISIBLE);
            binding.layoutDashboard.setVisibility(View.VISIBLE);
            binding.layoutAdminUsers.setVisibility(View.VISIBLE);
            // WP1.7：超期管理入口 — 图书管理员及以上可见
            binding.layoutOverdueMgmt.setVisibility(View.VISIBLE);
            binding.layoutAcquisition.setVisibility(
                    tm.isAcquisitorOrAbove() ? View.VISIBLE : View.GONE);
        } else if (loggedIn && tm.isAcquisitorOrAbove()) {
            binding.layoutAdminSection.setVisibility(View.VISIBLE);
            binding.layoutDashboard.setVisibility(View.GONE);
            binding.layoutAdminUsers.setVisibility(View.GONE);
            binding.layoutAcquisition.setVisibility(View.VISIBLE);
        } else {
            binding.layoutAdminSection.setVisibility(View.GONE);
        }

        if (loggedIn && profile != null) {
            // 已登录 + 已加载数据
            binding.tvLoginStatus.setText(tm.getRealName() != null ? tm.getRealName() : tm.getUsername());
            binding.tvUsername.setText(getString(R.string.profile_username_format, profile.getUsername()));
            binding.tvUsername.setVisibility(View.VISIBLE);
            binding.tvRealName.setText(getString(R.string.profile_realname_format, profile.getRealName() != null ? profile.getRealName() : ""));
            binding.tvRealName.setVisibility(View.VISIBLE);
            binding.tvEmail.setText(getString(R.string.profile_email_format, profile.getEmail() != null ? profile.getEmail() : ""));
            binding.tvEmail.setVisibility(View.VISIBLE);

            binding.layoutBorrowOverview.setVisibility(View.VISIBLE);
            binding.layoutFunctionEntries.setVisibility(View.VISIBLE);
            binding.btnLogin.setVisibility(View.GONE);
            binding.btnLogout.setVisibility(View.VISIBLE);
        } else if (loggedIn) {
            // 已登录但数据未加载
            binding.tvLoginStatus.setText(tm.getRealName() != null ? tm.getRealName() : tm.getUsername());
            binding.tvUsername.setText(getString(R.string.profile_username_format, tm.getUsername()));
            binding.tvUsername.setVisibility(View.VISIBLE);
            binding.layoutBorrowOverview.setVisibility(View.GONE);
            binding.layoutFunctionEntries.setVisibility(View.VISIBLE);
            binding.btnLogin.setVisibility(View.GONE);
            binding.btnLogout.setVisibility(View.VISIBLE);
        } else {
            // 未登录
            binding.tvLoginStatus.setText(getString(R.string.profile_not_logged_in));
            binding.tvUsername.setVisibility(View.GONE);
            binding.tvRealName.setVisibility(View.GONE);
            binding.tvEmail.setVisibility(View.GONE);
            binding.layoutBorrowOverview.setVisibility(View.GONE);
            binding.layoutFunctionEntries.setVisibility(View.GONE);
            binding.btnLogin.setVisibility(View.VISIBLE);
            binding.btnLogout.setVisibility(View.GONE);
        }
    }

    private void updateBorrowOverview(BorrowStatsVO stats) {
        if (stats == null) return;
        binding.tvCurrentBorrows.setText(String.valueOf(stats.getCurrentBorrows()));
        binding.tvHistoryCount.setText(String.valueOf(stats.getTotalBorrows()));
        binding.tvOverdueCount.setText(String.valueOf(stats.getTotalOverdue()));
    }

    /** P2-05：头像占位图品牌化 — 根据姓名/用户名首字母或默认图标渲染. */
    private void updateAvatar(boolean loggedIn, @Nullable UserProfile profile) {
        if (binding == null) return;

        String initials = null;
        if (loggedIn) {
            String source = null;
            if (profile != null) {
                source = profile.getRealName();
                if (source == null || source.trim().isEmpty()) {
                    source = profile.getUsername();
                }
            }
            if (source == null || source.trim().isEmpty()) {
                TokenManager tm = TokenManager.getInstance(requireContext());
                source = tm.getRealName();
                if (source == null || source.trim().isEmpty()) {
                    source = tm.getUsername();
                }
            }
            if (source != null && !source.trim().isEmpty()) {
                initials = extractInitial(source.trim());
            }
        }

        if (initials != null && !initials.isEmpty()) {
            binding.tvAvatarInitials.setText(initials);
            binding.tvAvatarInitials.setVisibility(View.VISIBLE);
            binding.ivAvatarDefault.setVisibility(View.GONE);
        } else {
            binding.tvAvatarInitials.setVisibility(View.GONE);
            binding.ivAvatarDefault.setVisibility(View.VISIBLE);
        }
    }

    /** 提取首字母：中文取首字，英文取首字母大写. */
    private String extractInitial(@NonNull String source) {
        char first = source.charAt(0);
        if (Character.isLetter(first)) {
            return String.valueOf(Character.toUpperCase(first));
        }
        // 非字母（如中文）直接返回首字符
        return String.valueOf(first);
    }

    /** 本地登出动作：清 token + 跳登录 + 清栈. */
    private void performLocalLogout(@NonNull View view) {
        TokenManager.getInstance(requireContext()).clear();
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .setLaunchSingleTop(true)
                .build();
        Navigation.findNavController(view).navigate(R.id.loginFragment, null, navOptions);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
