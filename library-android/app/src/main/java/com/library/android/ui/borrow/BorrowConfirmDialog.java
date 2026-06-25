package com.library.android.ui.borrow;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.snackbar.Snackbar;
import com.library.android.R;
import com.library.android.databinding.DialogBorrowConfirmBinding;
import com.library.android.model.BookSimpleVO;
import com.library.android.ui.common.Debounce;
import com.library.android.ui.common.NavArgKeys;
import com.library.android.viewmodel.BorrowViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 借阅确认底部弹窗（人员 B 主导）.
 *
 * <p>展示图书摘要、最大借阅数提示，确认后调用借阅 API。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class BorrowConfirmDialog extends BottomSheetDialogFragment {

    private DialogBorrowConfirmBinding binding;
    private BorrowViewModel viewModel;
    private BookSimpleVO book;

    /** 创建借阅确认弹窗实例. */
    public static BorrowConfirmDialog newInstance(BookSimpleVO book) {
        BorrowConfirmDialog dialog = new BorrowConfirmDialog();
        Bundle args = new Bundle();
        args.putParcelable(NavArgKeys.BOOK, book);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater,
                             @Nullable android.view.ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogBorrowConfirmBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(BorrowViewModel.class);

        if (getArguments() != null) {
            book = getArguments().getParcelable(NavArgKeys.BOOK);
        }

        displayBookInfo();
        setupButtons();
    }

    private void displayBookInfo() {
        if (book == null) return;
        binding.textBookTitle.setText(book.getTitle());
        binding.textAuthor.setText(book.getAuthor());
        binding.textIsbn.setText(book.getIsbn());
        binding.textHint.setVisibility(View.VISIBLE);
        binding.textHint.setText(getString(R.string.borrow_limit_hint));
    }

    private void setupButtons() {
        binding.btnCancel.setOnClickListener(v -> dismiss());

        // 防抖：快速双击不会发起两次借阅请求
        binding.btnConfirm.setOnClickListener(v -> {
            if (book == null || !Debounce.allow(v)) return;
            v.setEnabled(false);  // 配合防抖：请求期间彻底禁用
            viewModel.borrowBook(book.getId()).observe(getViewLifecycleOwner(), success -> {
                if (binding == null) return;
                v.setEnabled(true);
                if (Boolean.TRUE.equals(success)) {
                    // WP-14：先通知详情页（让详情页弹 Snackbar 因为 dialog 即将 dismiss），再 dismiss
                    Bundle result = new Bundle();
                    result.putBoolean("success", true);
                    result.putString("title", book.getTitle());
                    if (isAdded() && getParentFragment() != null) {
                        getParentFragment().getChildFragmentManager()
                                .setFragmentResult("borrow_result", result);
                    } else if (isAdded()) {
                        getParentFragmentManager().setFragmentResult("borrow_result", result);
                    }
                    dismiss();
                } else {
                    // 后端返回的具体失败原因（如"您已借阅该书，不可重复借阅"）
                    String msg = viewModel.getBorrowErrorMessage().getValue();
                    Snackbar.make(binding.getRoot(),
                            (msg != null && !msg.isEmpty()) ? msg : getString(R.string.borrow_failed),
                            Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
