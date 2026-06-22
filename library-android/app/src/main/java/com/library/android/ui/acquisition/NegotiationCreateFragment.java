package com.library.android.ui.acquisition;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.library.android.R;
import com.library.android.databinding.FragmentNegotiationCreateBinding;
import com.library.android.model.ElectronicResourceVO;
import com.library.android.model.SupplierVO;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.common.Debounce;
import com.library.android.ui.main.MainActivity;
import com.library.android.viewmodel.AcquisitionViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 智能谈判创建 Fragment — WP2.3 重构：供应商/资源数据通过 ViewModel 加载.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class NegotiationCreateFragment extends BaseFragment {

    private FragmentNegotiationCreateBinding binding;
    private AcquisitionViewModel viewModel;

    private final List<SupplierVO> suppliers = new ArrayList<>();
    private final List<ElectronicResourceVO> resources = new ArrayList<>();
    private long selectedResourceId = -1;
    private long selectedSupplierId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        com.library.android.ui.theme.ThemeManager.getInstance().setDarkMode(true);
        android.content.Context themedContext = com.library.android.ui.theme.ThemeManager.getInstance().wrapContext(requireContext());
        android.view.LayoutInflater themedInflater = inflater.cloneInContext(themedContext);
        binding = FragmentNegotiationCreateBinding.inflate(themedInflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AcquisitionViewModel.class);
        ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.acquisition_negotiation));

        // WP-14：AutoCompleteTextView 输入预选（用户可输入过滤，不再翻表找）
        // 修复：adapter item 格式为 "name（category）"，匹配时用 position 直接取 list
        binding.etResourceId.setOnItemClickListener((parent, v1, position, id) -> {
            if (position >= 0 && position < resources.size()) {
                selectedResourceId = resources.get(position).getId();
            }
        });
        binding.etSupplierId.setOnItemClickListener((parent, v12, position, id) -> {
            if (position >= 0 && position < suppliers.size()) {
                selectedSupplierId = suppliers.get(position).getId();
            }
        });

        binding.btnCreate.setOnClickListener(v -> {
            if (!Debounce.allow(v)) return;
            if (selectedResourceId <= 0 || selectedSupplierId <= 0) {
                binding.tvHint.setText(getString(R.string.acquisition_select_both));
                return;
            }
            v.setEnabled(false);
            viewModel.createNegotiation(selectedResourceId, selectedSupplierId);
        });

        observeError(viewModel.getErrorEvent());
        // WP-11：创建失败时重新启用按钮（原版仅成功 observer 恢复，失败后按钮永久禁用）
        viewModel.getErrorEvent().observe(getViewLifecycleOwner(), t -> {
            if (binding != null) binding.btnCreate.setEnabled(true);
        });

        viewModel.getNegotiationCreated().observe(getViewLifecycleOwner(), neg -> {
            if (neg == null || binding == null) return;
            binding.btnCreate.setEnabled(true);
            Bundle args = new Bundle();
            args.putLong("negotiationId", neg.getId());
            Navigation.findNavController(binding.getRoot())
                    .navigate(R.id.action_negotiationCreateFragment_to_negotiationDetailFragment, args);
        });

        // WP-11：数据到达后填充 AutoCompleteTextView 适配器
        viewModel.getSuppliers().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                suppliers.clear();
                suppliers.addAll(data);
                List<String> names = new ArrayList<>();
                for (SupplierVO s : data) names.add(s.getName() != null ? s.getName() : "");
                binding.etSupplierId.setAdapter(new android.widget.ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_list_item_1, names));
            }
        });
        viewModel.getResources().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                resources.clear();
                resources.addAll(data);
                List<String> names = new ArrayList<>();
                for (ElectronicResourceVO r : data) {
                    names.add(r.getName() + (r.getCategory() != null ? "（" + r.getCategory() + "）" : ""));
                }
                binding.etResourceId.setAdapter(new android.widget.ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_list_item_1, names));
            }
        });

        viewModel.loadSuppliers();
        viewModel.loadResources();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
