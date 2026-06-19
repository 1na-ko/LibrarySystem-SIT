package com.library.android.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.library.android.R;
import com.library.android.databinding.FragmentBorrowHistoryBinding;
import com.library.android.databinding.ItemHistoryBinding;
import com.library.android.model.BorrowRecordVO;
import com.library.android.model.PageResult;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.common.PagingScrollListener;
import com.library.android.ui.common.SafeStrings;
import com.library.android.ui.main.MainActivity;
import com.library.android.viewmodel.ProfileViewModel;

import java.util.ArrayList;
import java.util.Calendar;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 借阅历史 Fragment — 按年份筛选.
 *
 * <p>WP-8：补充分页（上拉加载）+ 列表项可点击跳详情 + observeError + 资源化.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class BorrowHistoryFragment extends BaseFragment {

    private FragmentBorrowHistoryBinding binding;
    private ProfileViewModel viewModel;
    private BorrowHistoryAdapter adapter;
    private Integer selectedYear;
    private PagingScrollListener scrollListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBorrowHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.page_title_borrow_history));

        adapter = new BorrowHistoryAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.rvHistory.setLayoutManager(layoutManager);
        binding.rvHistory.setAdapter(adapter);

        // WP-8：上拉加载更多
        scrollListener = new PagingScrollListener(layoutManager) {
            @Override
            protected void loadMore() {
                PageResult<BorrowRecordVO> page = viewModel.getCurrentHistoryPage();
                if (page != null && page.hasNextPage()) {
                    viewModel.loadBorrowHistoryMore(selectedYear, page.getPageNum() + 1);
                }
            }
        };
        binding.rvHistory.addOnScrollListener(scrollListener);

        setupYearFilter();
        setupObservers();
        observeError(viewModel.getErrorEvent());

        viewModel.loadBorrowHistory(null, 1);
    }

    private void setupYearFilter() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // "全部"选项
        Chip allChip = new Chip(requireContext());
        allChip.setText(R.string.tab_all);
        allChip.setCheckable(true);
        allChip.setChecked(true);
        allChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedYear = null;
                viewModel.loadBorrowHistory(null, 1);
            }
        });
        binding.chipGroupYears.addView(allChip);

        // 近 5 年
        for (int i = 0; i < 5; i++) {
            int year = currentYear - i;
            Chip chip = new Chip(requireContext());
            chip.setText(String.valueOf(year));
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedYear = year;
                    viewModel.loadBorrowHistory(year, 1);
                }
            });
            binding.chipGroupYears.addView(chip);
        }
    }

    private void setupObservers() {
        viewModel.getBorrowHistory().observe(getViewLifecycleOwner(), page -> {
            if (binding == null) return;
            if (page != null && page.getRecords() != null && !page.getRecords().isEmpty()) {
                adapter.submitList(page.getRecords());
                binding.layoutEmpty.setVisibility(View.GONE);
            } else {
                adapter.submitList(new ArrayList<>());
                binding.layoutEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null && scrollListener != null) {
            binding.rvHistory.removeOnScrollListener(scrollListener);
        }
        binding = null;
    }

    // ======================== BorrowHistoryAdapter ========================

    private static class BorrowHistoryAdapter extends BaseAdapter<BorrowRecordVO, ItemHistoryBinding> {

        BorrowHistoryAdapter() {
            super(R.layout.item_history, new DiffUtil.ItemCallback<BorrowRecordVO>() {
                @Override
                public boolean areItemsTheSame(@NonNull BorrowRecordVO oldItem, @NonNull BorrowRecordVO newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull BorrowRecordVO oldItem, @NonNull BorrowRecordVO newItem) {
                    String oldStatus = oldItem.getStatus() != null ? oldItem.getStatus() : "";
                    String newStatus = newItem.getStatus() != null ? newItem.getStatus() : "";
                    return java.util.Objects.equals(oldStatus, newStatus);
                }
            });
        }

        @Override
        protected ItemHistoryBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return ItemHistoryBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(ItemHistoryBinding binding, BorrowRecordVO item, int position) {
            if (item.getBook() != null) {
                binding.tvBookTitle.setText(SafeStrings.defaultIfEmpty(item.getBook().getTitle()));
            }
            binding.tvDate.setText(SafeStrings.safeMonth(item.getBorrowDate()));
            binding.tvDuration.setText(binding.getRoot().getContext().getString(R.string.borrow_duration_format,
                    SafeStrings.defaultIfEmpty(item.getBorrowDate(), "—"),
                    SafeStrings.defaultIfEmpty(item.getDueDate(), "—")));

            String statusText;
            int statusColorRes;
            String status = item.getStatus() != null ? item.getStatus() : "";
            switch (status) {
                case "BORROWED":
                case "RENEWED":
                    statusText = item.isOverdue()
                            ? binding.getRoot().getContext().getString(R.string.borrow_overdue_status)
                            : binding.getRoot().getContext().getString(R.string.borrow_active_status);
                    statusColorRes = item.isOverdue() ? R.color.chip_overdue : R.color.chip_borrowed;
                    break;
                case "RETURNED":
                    statusText = binding.getRoot().getContext().getString(R.string.borrow_returned_status);
                    statusColorRes = R.color.chip_returned;
                    break;
                case "OVERDUE":
                    statusText = binding.getRoot().getContext().getString(R.string.borrow_overdue_status);
                    statusColorRes = R.color.chip_overdue;
                    break;
                case "LOST":
                    statusText = binding.getRoot().getContext().getString(R.string.borrow_lost_status);
                    statusColorRes = R.color.chip_overdue;
                    break;
                default:
                    statusText = status;
                    statusColorRes = R.color.chip_borrowed;
            }
            binding.tvStatus.setText(statusText);
            binding.tvStatus.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(
                            binding.getRoot().getContext(), statusColorRes));

            // WP-8：列表项可点击跳借阅详情（带 borrowId）
            binding.getRoot().setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putLong("borrowId", item.getId());
                Navigation.findNavController(v).navigate(R.id.borrowDetailFragment, args);
            });
        }
    }
}
