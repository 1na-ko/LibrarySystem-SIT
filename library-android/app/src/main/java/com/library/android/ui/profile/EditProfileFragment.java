package com.library.android.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.library.android.databinding.FragmentEditProfileBinding;
import com.library.android.ui.main.MainActivity;
import com.library.android.viewmodel.ProfileViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 编辑资料 Fragment.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class EditProfileFragment extends Fragment {

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

        ((MainActivity) requireActivity()).setGlobalTitle("编辑资料");

        // 加载当前用户信息
        viewModel.getUserProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                binding.etEmail.setText(profile.getEmail() != null ? profile.getEmail() : "");
                binding.etPhone.setText(profile.getPhone() != null ? profile.getPhone() : "");
            }
        });

        if (viewModel.getUserProfile().getValue() == null) {
            viewModel.loadProfile();
        }

        binding.btnSave.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String phone = binding.etPhone.getText().toString().trim();
            viewModel.updateProfile(email, phone);
        });

        viewModel.isSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                requireActivity().onBackPressed();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}