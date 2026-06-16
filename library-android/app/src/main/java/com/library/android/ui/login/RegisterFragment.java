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

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.btnRegister.setEnabled(!loading);
        });

        binding.btnRegister.setOnClickListener(v -> {
            String username = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String realName = binding.etRealName.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty() || realName.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "请填写所有必填项", Toast.LENGTH_SHORT).show();
                return;
            }

            viewModel.register(username, password, realName, email, "");
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