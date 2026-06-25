package com.library.android.ui.profile;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.library.android.R;
import com.library.android.databinding.FragmentEditProfileBinding;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.main.MainActivity;
import com.library.android.viewmodel.ProfileViewModel;

import java.util.regex.Pattern;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 编辑资料 Fragment.
 *
 * <p>WP-8：补充 email/phone 格式校验 + 保存失败提示 + 保存期间禁用防重.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class EditProfileFragment extends BaseFragment {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private FragmentEditProfileBinding binding;
    private ProfileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.page_title_edit_profile));

        // 加载当前用户信息
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null && binding != null) {
                binding.etEmail.setText(profile.getEmail() != null ? profile.getEmail() : "");
                binding.etPhone.setText(profile.getPhone() != null ? profile.getPhone() : "");
            }
        });

        if (viewModel.getUserProfile().getValue() == null) {
            viewModel.loadProfile();
        }

        binding.btnSave.setOnClickListener(v -> {
            String email = binding.etEmail.getText() != null
                    ? binding.etEmail.getText().toString().trim() : "";
            String phone = binding.etPhone.getText() != null
                    ? binding.etPhone.getText().toString().trim() : "";

            // WP-8：输入校验
            if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.etEmail.setError(getString(R.string.profile_email_invalid));
                return;
            }
            if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
                binding.etPhone.setError(getString(R.string.profile_phone_invalid));
                return;
            }

            // WP-8：保存期间禁用防重复提交
            v.setEnabled(false);
            viewModel.updateProfile(email.isEmpty() ? null : email, phone.isEmpty() ? null : phone);
        });

        viewModel.isSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (binding == null) return;
            if (Boolean.TRUE.equals(success)) {
                requireActivity().onBackPressed();
            } else {
                // WP-8：保存失败提示 + 重新启用按钮
                binding.btnSave.setEnabled(true);
                Snackbar.make(binding.getRoot(), R.string.profile_save_failed, Snackbar.LENGTH_LONG).show();
            }
        });

        // 错误事件也重新启用按钮
        viewModel.getErrorEvent().observe(getViewLifecycleOwner(), t -> {
            if (binding != null) binding.btnSave.setEnabled(true);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
