package com.library.android.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
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
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // 登录按钮
        binding.btnLogin.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.loginFragment));

        // 退出登录
        binding.btnLogout.setOnClickListener(v -> {
            TokenManager.getInstance(requireContext()).clear();
            updateUI(null);
        });

        // 功能入口点击
        binding.layoutBorrowHistory.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_borrowHistoryFragment));

        binding.layoutBorrowStats.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_borrowStatsFragment));

        binding.layoutRecommendations.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_recommendationsFragment));

        binding.layoutEditProfile.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_editProfileFragment));

        binding.layoutReservations.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_profileFragment_to_reservationListFragment));

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
        if (tm.isLoggedIn() && profile != null) {
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
        } else if (tm.isLoggedIn()) {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}