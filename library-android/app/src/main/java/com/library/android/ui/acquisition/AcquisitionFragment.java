package com.library.android.ui.acquisition;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.library.android.R;
import com.library.android.databinding.FragmentAcquisitionBinding;
import com.library.android.ui.main.MainActivity;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 智能采编模块入口聚合页（C.1 新增）— 4 张卡片入口跳转 4 个子页.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class AcquisitionFragment extends Fragment {

    private FragmentAcquisitionBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        com.library.android.ui.theme.ThemeManager.getInstance().setDarkMode(true);
        android.content.Context themedContext = com.library.android.ui.theme.ThemeManager.getInstance().wrapContext(requireContext());
        android.view.LayoutInflater themedInflater = inflater.cloneInContext(themedContext);
        binding = FragmentAcquisitionBinding.inflate(themedInflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.acquisition_title));

        binding.cardPredict.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(R.id.action_acquisitionFragment_to_purchasePredictFragment));
        binding.cardDuplicate.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(R.id.action_acquisitionFragment_to_duplicateCheckFragment));
        binding.cardGap.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(R.id.action_acquisitionFragment_to_gapAnalysisFragment));
        binding.cardNegotiation.setOnClickListener(v -> Navigation.findNavController(v)
                .navigate(R.id.action_acquisitionFragment_to_negotiationCreateFragment));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
