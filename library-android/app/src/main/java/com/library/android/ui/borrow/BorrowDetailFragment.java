package com.library.android.ui.borrow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.library.android.R;
import com.library.android.databinding.FragmentBorrowDetailBinding;
import com.library.android.model.BorrowRecordVO;
import com.library.android.viewmodel.BorrowDetailViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 借阅详情 Fragment（人员 B 主导）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class BorrowDetailFragment extends Fragment {

    private FragmentBorrowDetailBinding binding;
    private BorrowDetailViewModel viewModel;
    private long borrowId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBorrowDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BorrowDetailViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        if (getArguments() != null) {
            borrowId = getArguments().getLong("borrowId", 0);
        }

        setupButtons();
        observeViewModel();
        viewModel.loadDetail(borrowId);
    }

    private void setupButtons() {
        binding.btnRenew.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.confirm_renew_title)
                    .setMessage(R.string.confirm_renew_message)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> viewModel.renewBook(borrowId))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        binding.btnReturn.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.confirm_return_title)
                    .setMessage(R.string.confirm_return_message)
                    .setPositiveButton(R.string.confirm, (dialog, which) -> viewModel.returnBook(borrowId))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            // handled by progress bar if needed
        });

        viewModel.getBorrowDetail().observe(getViewLifecycleOwner(), this::displayDetail);

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            }
        });

        viewModel.getReturnSuccess().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Snackbar.make(binding.getRoot(), R.string.return_success, Snackbar.LENGTH_SHORT).show();
                binding.layoutActions.setVisibility(View.GONE);
            }
        });

        viewModel.getRenewResult().observe(getViewLifecycleOwner(), newDueDate -> {
            if (newDueDate != null) {
                Snackbar.make(binding.getRoot(),
                        getString(R.string.renew_success_format, newDueDate),
                        Snackbar.LENGTH_SHORT).show();
                viewModel.loadDetail(borrowId); // 刷新
            } else if (newDueDate == null && viewModel.getErrorMessage().getValue() == null) {
                // renewResult set to null means failure
            }
        });
    }

    private void displayDetail(BorrowRecordVO detail) {
        if (detail == null) return;

        // 图书信息
        if (detail.getBook() != null) {
            binding.textBookTitle.setText(detail.getBook().getTitle());
            binding.textAuthor.setText(detail.getBook().getAuthor());
            binding.textIsbn.setText(getString(R.string.isbn_format, detail.getBook().getIsbn()));
        }

        // 日期信息
        binding.textBorrowDate.setText(detail.getBorrowDate());
        binding.textDueDate.setText(detail.getDueDate());

        // 归还日期
        if (detail.getReturnDate() != null && !detail.getReturnDate().isEmpty()) {
            binding.layoutReturnDate.setVisibility(View.VISIBLE);
            binding.textReturnDate.setText(detail.getReturnDate());
        }

        // 状态
        binding.textStatus.setText(getStatusText(detail.getStatus()));

        // 续借次数
        binding.textRenewCount.setText(getString(R.string.renew_count_format, detail.getRenewCount()));

        // 罚款
        if (detail.getFineAmount() != null && detail.getFineAmount() > 0) {
            binding.layoutFine.setVisibility(View.VISIBLE);
            binding.textFine.setText(getString(R.string.fine_format, detail.getFineAmount()));
        }

        // 操作按钮可见性
        if (detail.isReturned()) {
            binding.layoutActions.setVisibility(View.GONE);
        } else {
            binding.layoutActions.setVisibility(View.VISIBLE);
            binding.btnRenew.setEnabled(detail.canRenew());
        }
    }

    private String getStatusText(String status) {
        if (status == null) return "";
        switch (status) {
            case "BORROWED": return getString(R.string.status_borrowed);
            case "RENEWED": return getString(R.string.status_renewed);
            case "RETURNED": return getString(R.string.status_returned);
            case "OVERDUE": return getString(R.string.status_overdue);
            default: return status;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}