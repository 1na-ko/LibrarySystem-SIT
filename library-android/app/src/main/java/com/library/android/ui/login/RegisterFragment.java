package com.library.android.ui.login;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.library.android.R;
import com.library.android.databinding.FragmentRegisterBinding;
import com.library.android.viewmodel.RegisterViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 注册页面 Fragment.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class RegisterFragment extends Fragment {

    private static final String TAG = "RegisterFragment";
    private FragmentRegisterBinding binding;
    private RegisterViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        viewModel.isRegisterSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(requireContext(), "注册成功，请登录", Toast.LENGTH_LONG).show();
                Navigation.findNavController(view).popBackStack();
            }
        });

        viewModel.getErrorEvent().observe(getViewLifecycleOwner(), throwable -> {
            if (throwable != null && throwable.getMessage() != null && !throwable.getMessage().isEmpty()) {
                Toast.makeText(requireContext(), throwable.getMessage() != null ? throwable.getMessage() : "", Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getLoadingState().observe(getViewLifecycleOwner(), state -> {
            binding.btnRegister.setEnabled(state != com.library.android.ui.common.LoadingState.LOADING);
        });

        binding.btnRegister.setOnClickListener(v -> {
            String username = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String realName = binding.etRealName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String phone = binding.etPhone.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty() || realName.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.register_fill_required), Toast.LENGTH_SHORT).show();
                return;
            }
            // B.8 修复：手机号格式校验（与后端 ^1[3-9]\d{9}$ 对齐，选填）
            if (!phone.isEmpty() && !phone.matches("^1[3-9]\\d{9}$")) {
                Toast.makeText(requireContext(), getString(R.string.register_phone_invalid), Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.register(username, password, realName, email, phone);
        });

        binding.btnBackToLogin.setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}