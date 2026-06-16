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
import com.library.android.model.BookVO;
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
    private BookVO book;

    /** 创建借阅确认弹窗实例. */
    public static BorrowConfirmDialog newInstance(BookVO book) {
        BorrowConfirmDialog dialog = new BorrowConfirmDialog();
        Bundle args = new Bundle();
        args.putSerializable("book", book);
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
            book = (BookVO) getArguments().getSerializable("book");
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

        binding.btnConfirm.setOnClickListener(v -> {
            if (book != null) {
                viewModel.borrowBook(book.getId()).observe(this, success -> {
                    if (Boolean.TRUE.equals(success)) {
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.borrow_success_format, book.getTitle()),
                                Snackbar.LENGTH_SHORT).show();
                        dismiss();
                    } else {
                        Snackbar.make(binding.getRoot(),
                                R.string.borrow_failed, Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
