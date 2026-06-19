package com.library.android.ui.acquisition;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.library.android.R;
import com.library.android.databinding.FragmentGapAnalysisBinding;
import com.library.android.databinding.ItemDuplicateBinding;
import com.library.android.model.CategoryVO;
import com.library.android.model.GapAnalysisResult;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.main.MainActivity;
import com.library.android.viewmodel.AcquisitionViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 学科缺口分析 Fragment.
 *
 * <p>WP-11：由纯文本骨架升级为结构化 UI（覆盖率进度条 + 缺口图书列表卡片 + 优先级颜色标记）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class GapAnalysisFragment extends BaseFragment {

    private FragmentGapAnalysisBinding binding;
    private AcquisitionViewModel viewModel;

    private final List<CategoryVO> flatCategories = new ArrayList<>();
    private long selectedSubjectId = -1;
    private GapBookAdapter gapAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context themedContext = com.library.android.ui.theme.ThemeManager.getInstance().wrapContext(requireContext());
        android.view.LayoutInflater themedInflater = inflater.cloneInContext(themedContext);
        binding = FragmentGapAnalysisBinding.inflate(themedInflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AcquisitionViewModel.class);
        ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.acquisition_gap));

        gapAdapter = new GapBookAdapter();
        binding.rvGapBooks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvGapBooks.setAdapter(gapAdapter);

        // WP-11：AutoCompleteTextView 输入预选分类
        binding.etSubjectId.setOnItemClickListener((parent, v1, pos, id) -> {
            String name = (String) parent.getItemAtPosition(pos);
            for (CategoryVO c : flatCategories) {
                if (c.getName() != null && c.getName().equals(name)) {
                    selectedSubjectId = c.getId();
                    break;
                }
            }
        });

        binding.btnRun.setOnClickListener(v -> {
            if (selectedSubjectId <= 0) {
                binding.inputSubjectId.setError(getString(R.string.acquisition_select_subject));
                return;
            }
            binding.inputSubjectId.setError(null);
            viewModel.analyzeGap(selectedSubjectId);
        });

        observeError(viewModel.getErrorEvent());
        viewModel.getGapResult().observe(getViewLifecycleOwner(), this::renderResult);

        viewModel.getCategories().observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                flatCategories.clear();
                flatten(data);
                List<String> names = new ArrayList<>();
                for (CategoryVO c : flatCategories) {
                    names.add(c.getName() != null ? c.getName() : "");
                }
                binding.etSubjectId.setAdapter(new android.widget.ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_list_item_1, names));
            }
        });

        viewModel.loadCategories();
    }

    private void flatten(List<CategoryVO> nodes) {
        if (nodes == null) return;
        for (CategoryVO node : nodes) {
            flatCategories.add(node);
            if (node.getChildren() != null) flatten(node.getChildren());
        }
    }

    private void renderResult(GapAnalysisResult r) {
        if (binding == null || r == null) return;
        binding.cardOverview.setVisibility(View.VISIBLE);
        binding.tvSubjectName.setText(r.getSubjectName() != null ? r.getSubjectName() : String.valueOf(r.getSubjectId()));
        binding.tvOwned.setText(getString(R.string.acquisition_gap_owned, r.getOwnedBooks(), r.getTotalCoreBooks()));
        binding.tvTotal.setText(getString(R.string.acquisition_gap_total, r.getTotalCoreBooks()));
        int covPct = (int) Math.round(r.getCoverage() * 100);
        binding.progressCoverage.setProgress(covPct);
        binding.tvCoverage.setText(getString(R.string.acquisition_coverage_format, covPct));

        // WP-0/4：message 非空时展示友好提示（如"暂无核心书目数据"）
        if (r.getMessage() != null && !r.getMessage().isEmpty()) {
            binding.tvMessage.setText(r.getMessage());
            binding.tvMessage.setVisibility(View.VISIBLE);
        } else {
            binding.tvMessage.setVisibility(View.GONE);
        }

        // 缺口图书列表
        List<GapAnalysisResult.GapBook> gaps = r.getGapBooks() != null ? r.getGapBooks() : new ArrayList<>();
        binding.tvGapHeader.setVisibility(gaps.isEmpty() ? View.GONE : View.VISIBLE);
        gapAdapter.submitList(gaps);

        binding.tvResultHint.setVisibility(
                (r.getTotalCoreBooks() == 0 || gaps.isEmpty()) && binding.tvMessage.getVisibility() == View.GONE
                        ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ======================== GapBookAdapter ========================

    private static class GapBookAdapter extends BaseAdapter<GapAnalysisResult.GapBook, ItemDuplicateBinding> {

        GapBookAdapter() {
            super(R.layout.item_duplicate, new DiffUtil.ItemCallback<GapAnalysisResult.GapBook>() {
                @Override
                public boolean areItemsTheSame(@NonNull GapAnalysisResult.GapBook o,
                                               @NonNull GapAnalysisResult.GapBook n) {
                    return java.util.Objects.equals(o.getIsbn(), n.getIsbn());
                }

                @Override
                public boolean areContentsTheSame(@NonNull GapAnalysisResult.GapBook o,
                                                  @NonNull GapAnalysisResult.GapBook n) {
                    return java.util.Objects.equals(o.getPriority(), n.getPriority());
                }
            });
        }

        @Override
        protected ItemDuplicateBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return ItemDuplicateBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(ItemDuplicateBinding b, GapAnalysisResult.GapBook item, int position) {
            b.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "");
            b.tvAuthor.setText(item.getAuthor() != null ? item.getAuthor() : "");
            b.tvIsbn.setText(item.getIsbn() != null
                    ? b.getRoot().getContext().getString(R.string.isbn_format, item.getIsbn()) : "");
            String priority = item.getPriority() != null ? item.getPriority() : "LOW";
            b.chipStrategy.setText(priority);
            int chipColor;
            switch (priority) {
                case "CRITICAL": chipColor = R.color.error; break;
                case "HIGH": chipColor = R.color.chip_overdue; break;
                case "MEDIUM": chipColor = R.color.accent_reserve; break;
                default: chipColor = R.color.accent_available; break;
            }
            b.chipStrategy.setChipBackgroundColorResource(chipColor);
            b.chipStrategy.setTextColor(
                    androidx.core.content.ContextCompat.getColor(b.getRoot().getContext(), R.color.bg_card));
            b.progressScore.setVisibility(View.GONE);
            b.tvScore.setText("");
        }
    }
}
