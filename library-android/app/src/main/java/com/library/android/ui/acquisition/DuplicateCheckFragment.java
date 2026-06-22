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
import com.library.android.databinding.FragmentDuplicateCheckBinding;
import com.library.android.databinding.ItemDuplicateBinding;
import com.library.android.model.DuplicateCheckResult;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.main.MainActivity;
import com.library.android.viewmodel.AcquisitionViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 图书查重 Fragment.
 *
 * <p>WP-11：由纯文本骨架升级为结构化卡片列表（书名/作者/ISBN + 匹配策略标签 + 相似度进度条）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class DuplicateCheckFragment extends BaseFragment {

    private FragmentDuplicateCheckBinding binding;
    private AcquisitionViewModel viewModel;
    private DuplicateAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        com.library.android.ui.theme.ThemeManager.getInstance().setDarkMode(true);
        android.content.Context themedContext = com.library.android.ui.theme.ThemeManager.getInstance().wrapContext(requireContext());
        android.view.LayoutInflater themedInflater = inflater.cloneInContext(themedContext);
        binding = FragmentDuplicateCheckBinding.inflate(themedInflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AcquisitionViewModel.class);
        ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.acquisition_duplicate));

        adapter = new DuplicateAdapter();
        binding.rvDuplicates.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDuplicates.setAdapter(adapter);

        binding.btnRun.setOnClickListener(v -> {
            String isbn = binding.etIsbn.getText() != null ? binding.etIsbn.getText().toString().trim() : "";
            String title = binding.etTitle.getText() != null ? binding.etTitle.getText().toString().trim() : "";
            String author = binding.etAuthor.getText() != null ? binding.etAuthor.getText().toString().trim() : "";
            if (isbn.isEmpty() && title.isEmpty()) {
                binding.inputTitle.setError(getString(R.string.acquisition_enter_isbn_or_title));
                return;
            }
            binding.inputTitle.setError(null);
            viewModel.checkDuplicate(isbn.isEmpty() ? null : isbn,
                    title.isEmpty() ? null : title, author.isEmpty() ? null : author);
        });

        observeError(viewModel.getErrorEvent());
        viewModel.getDuplicateResult().observe(getViewLifecycleOwner(), this::renderResult);
    }

    private void renderResult(DuplicateCheckResult result) {
        if (binding == null || result == null) return;
        List<DuplicateCheckResult.DuplicateItem> items =
                result.getDuplicates() != null ? result.getDuplicates() : new ArrayList<>();

        // 状态横幅
        binding.tvStatusBanner.setVisibility(View.VISIBLE);
        if (result.isDuplicate()) {
            binding.tvStatusBanner.setText(getString(R.string.acquisition_duplicate_found, items.size()));
            binding.tvStatusBanner.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.chip_overdue));
            binding.tvStatusBanner.setTextColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.bg_card));
        } else {
            binding.tvStatusBanner.setText(R.string.acquisition_no_duplicate);
            binding.tvStatusBanner.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.accent_available));
            binding.tvStatusBanner.setTextColor(
                    androidx.core.content.ContextCompat.getColor(requireContext(), R.color.bg_card));
        }

        binding.tvResultHint.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submitList(items);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ======================== DuplicateAdapter ========================

    private static class DuplicateAdapter extends BaseAdapter<DuplicateCheckResult.DuplicateItem, ItemDuplicateBinding> {

        DuplicateAdapter() {
            super(R.layout.item_duplicate, new DiffUtil.ItemCallback<DuplicateCheckResult.DuplicateItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull DuplicateCheckResult.DuplicateItem o,
                                               @NonNull DuplicateCheckResult.DuplicateItem n) {
                    return (o.getBook() != null && n.getBook() != null && o.getBook().getId() == n.getBook().getId())
                            || o == n;
                }

                @Override
                public boolean areContentsTheSame(@NonNull DuplicateCheckResult.DuplicateItem o,
                                                  @NonNull DuplicateCheckResult.DuplicateItem n) {
                    return Double.compare(o.getScore(), n.getScore()) == 0
                            && java.util.Objects.equals(o.getMatchStrategy(), n.getMatchStrategy());
                }
            });
        }

        @Override
        protected ItemDuplicateBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return ItemDuplicateBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(ItemDuplicateBinding b, DuplicateCheckResult.DuplicateItem item, int position) {
            if (item.getBook() != null) {
                b.tvTitle.setText(item.getBook().getTitle() != null ? item.getBook().getTitle() : "");
                b.tvAuthor.setText(item.getBook().getAuthor() != null ? item.getBook().getAuthor() : "");
                b.tvIsbn.setText(item.getBook().getIsbn() != null
                        ? b.getRoot().getContext().getString(R.string.isbn_format, item.getBook().getIsbn())
                        : "");
            }
            String strategy = item.getMatchStrategy() != null ? item.getMatchStrategy() : "";
            b.chipStrategy.setText(strategy);
            int pct = (int) Math.round(item.getScore() * 100);
            b.progressScore.setProgress(pct);
            b.tvScore.setText(b.getRoot().getContext().getString(R.string.acquisition_similarity_format, pct));
        }
    }
}
