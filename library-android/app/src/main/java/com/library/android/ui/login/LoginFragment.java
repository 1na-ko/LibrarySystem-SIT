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
import com.library.android.databinding.FragmentLoginBinding;
import com.library.android.viewmodel.LoginViewModel;

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
                Navigation.findNavController(view)
                        .popBackStack(R.id.searchFragment, false);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            Log.d(TAG, "错误消息: " + msg);
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                binding.etUsername.setError(msg);
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            Log.d(TAG, "加载状态: loading=" + loading);
            binding.btnLogin.setEnabled(!loading);
            binding.btnRegister.setEnabled(!loading);
        });

        binding.btnLogin.setOnClickListener(v -> {
            String username = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            Log.d(TAG, "点击登录: username=" + username + ", password=" + (password.isEmpty() ? "空" : "已填写"));
            viewModel.login(requireContext(), username, password);
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
}