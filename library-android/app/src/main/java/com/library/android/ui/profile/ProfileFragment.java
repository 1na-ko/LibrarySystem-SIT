package com.library.android.ui.profile;

import android.os.Bundle;
import android.util.Log;
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
import com.library.android.repository.AuthRepository;
import com.library.android.viewmodel.ProfileViewModel;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 个人中心 Fragment — 含用户信息卡片、借阅概览、功能入口.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    AuthRepository authRepository;

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
        // 原版用 new ViewModelProvider(this) 创建 Fragment 私有实例，
        // 导致 EditProfileFragment 修改的资料不会同步到 ProfileFragment（同名不同实例）.
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // 立即同步设置初始 UI，避免 XML 默认值导致"未登录"闪现
        updateUI(viewModel.getUserProfile().getValue());

        // 登录按钮 — WP-1：加 NavOptions 防栈叠加 + launchSingleTop
        binding.btnLogin.setOnClickListener(v -> {
            androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .build();
            Navigation.findNavController(view).navigate(R.id.loginFragment, null, navOptions);
        });

        // 退出登录：先调后端 logout（命中 AT 黑名单 OWASP），再清本地 token + 跳转登录
        binding.btnLogout.setOnClickListener(v -> {
            v.setEnabled(false);  // 防抖：避免快速双击重复登出
            disposables.add(authRepository.logout()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            result -> performLocalLogout(view),
                            throwable -> {
                                // 后端不可达时仍要保证本地清退，避免用户卡死
                                Log.w(TAG, "后端 logout 失败，仍执行本地登出", throwable);
                                performLocalLogout(view);
                            }
                    ));
        });

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
            binding.tvUsername.setText("用户名: " + profile.getUsername());
            binding.tvUsername.setVisibility(View.VISIBLE);
            binding.tvRealName.setText("姓名: " + (profile.getRealName() != null ? profile.getRealName() : ""));
            binding.tvRealName.setVisibility(View.VISIBLE);
            binding.tvEmail.setText("邮箱: " + (profile.getEmail() != null ? profile.getEmail() : ""));
            binding.tvEmail.setVisibility(View.VISIBLE);

            binding.layoutBorrowOverview.setVisibility(View.VISIBLE);
            binding.layoutFunctionEntries.setVisibility(View.VISIBLE);
            binding.btnLogin.setVisibility(View.GONE);
            binding.btnLogout.setVisibility(View.VISIBLE);
        } else if (loggedIn) {
            // 已登录但数据未加载
            binding.tvLoginStatus.setText(tm.getRealName() != null ? tm.getRealName() : tm.getUsername());
            binding.tvUsername.setText("用户名: " + tm.getUsername());
            binding.tvUsername.setVisibility(View.VISIBLE);
            binding.layoutBorrowOverview.setVisibility(View.GONE);
            binding.layoutFunctionEntries.setVisibility(View.VISIBLE);
            binding.btnLogin.setVisibility(View.GONE);
            binding.btnLogout.setVisibility(View.VISIBLE);
        } else {
            // 未登录
            binding.tvLoginStatus.setText("未登录");
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
        disposables.clear();
        binding = null;
    }
}