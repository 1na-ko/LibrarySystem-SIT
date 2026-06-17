package com.library.android.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.library.android.R;
import com.library.android.databinding.FragmentBorrowHistoryBinding;
import com.library.android.databinding.ItemHistoryBinding;
import com.library.android.ui.main.MainActivity;
import com.library.android.model.BorrowRecordVO;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.viewmodel.ProfileViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 借阅历史 Fragment — 按年份筛选.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class BorrowHistoryFragment extends Fragment {

    private FragmentBorrowHistoryBinding binding;
    private ProfileViewModel viewModel;
    private BorrowHistoryAdapter adapter;
    private Integer selectedYear;

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

        ((MainActivity) requireActivity()).setGlobalTitle("借阅历史");

        adapter = new BorrowHistoryAdapter();
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHistory.setAdapter(adapter);

        setupYearFilter();
        setupObservers();

        viewModel.loadBorrowHistory(null, 1);
    }

    private void setupYearFilter() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // "全部"选项
        Chip allChip = new Chip(requireContext());
        allChip.setText("全部");
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
                    return oldItem.getStatus().equals(newItem.getStatus());
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
                binding.tvBookTitle.setText(item.getBook().getTitle());
            }
            binding.tvDate.setText(item.getBorrowDate() != null
                    ? item.getBorrowDate().substring(0, Math.min(7, item.getBorrowDate().length())) : "");
            binding.tvDuration.setText("借阅: " + item.getBorrowDate() + " — " + item.getDueDate());

            String statusText;
            int statusColorRes;
            switch (item.getStatus()) {
                case "BORROWED":
                case "RENEWED":
                    statusText = item.isOverdue() ? "已超期" : "借阅中";
                    statusColorRes = item.isOverdue() ? R.color.chip_overdue : R.color.chip_borrowed;
                    break;
                case "RETURNED":
                    statusText = "已归还";
                    statusColorRes = R.color.chip_returned;
                    break;
                default:
                    statusText = item.getStatus();
                    statusColorRes = R.color.chip_borrowed;
            }
            binding.tvStatus.setText(statusText);
            binding.tvStatus.setBackgroundColor(
                    androidx.core.content.ContextCompat.getColor(
                            binding.getRoot().getContext(), statusColorRes));
        }
    }
}