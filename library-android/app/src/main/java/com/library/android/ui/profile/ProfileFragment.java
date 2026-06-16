package com.library.android.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.library.android.R;
import com.library.android.databinding.FragmentProfileBinding;
import com.library.android.util.TokenManager;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 个人中心 Fragment.
 *
 * <p>使用 Hilt @AndroidEntryPoint，后续可通过 @Inject 注入依赖.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

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
        updateUI();

        binding.btnLogin.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.loginFragment));

        binding.btnLogout.setOnClickListener(v -> {
            TokenManager.getInstance(requireContext()).clear();
            updateUI();
        });
    }

    /**
     * 根据登录状态切换界面.
     */
    private void updateUI() {
        TokenManager tm = TokenManager.getInstance(requireContext());
        if (tm.isLoggedIn()) {
            binding.tvLoginStatus.setText("已登录");
            binding.tvUsername.setVisibility(View.VISIBLE);
            binding.tvUsername.setText("用户名: " + tm.getUsername());
            binding.btnLogin.setVisibility(View.GONE);
            binding.btnLogout.setVisibility(View.VISIBLE);
        } else {
            binding.tvLoginStatus.setText("未登录");
            binding.tvUsername.setVisibility(View.GONE);
            binding.btnLogin.setVisibility(View.VISIBLE);
            binding.btnLogout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}