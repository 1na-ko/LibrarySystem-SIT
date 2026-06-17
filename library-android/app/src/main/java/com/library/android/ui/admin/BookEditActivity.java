package com.library.android.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputLayout;
import com.library.android.R;
import com.library.android.databinding.ActivityBookEditBinding;
import com.library.android.ui.theme.ThemeManager;
import com.library.android.model.BookCreateRequest;
import com.library.android.model.BookUpdateRequest;
import com.library.android.model.BookVO;
import com.library.android.viewmodel.AdminViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 图书编目 Activity — 新增/修改图书（人员 B 主导）.
 *
 * <p>通过 Intent extra "bookId" 区分新增（bookId=-1）和编辑模式。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class BookEditActivity extends AppCompatActivity {

    private ActivityBookEditBinding binding;
    private AdminViewModel viewModel;

    private boolean isEditMode = false;
    private long editBookId = -1;
    private long categoryId = 1; // 默认分类 ID，后续可从分类选择器传入

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.getInstance().setDarkMode(true);
        setTheme(R.style.Theme_LibrarySystem_Dark);
        super.onCreate(savedInstanceState);
        binding = ActivityBookEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        // 判断编辑模式
        editBookId = getIntent().getLongExtra("bookId", -1);
        isEditMode = editBookId > 0;

        if (isEditMode) {
            setTitle(R.string.edit_book_title);
            binding.btnDelete.setVisibility(View.VISIBLE);
            binding.btnDelete.setOnClickListener(v -> confirmDelete());
            // 加载现有图书数据（简化处理，实际应从 API 获取后填充）
        } else {
            setTitle(R.string.add_book_title);
            binding.btnDelete.setVisibility(View.GONE);
        }

        binding.btnSave.setOnClickListener(v -> saveBook());
        observeViewModel();
    }

    private void saveBook() {
        if (!validateInputs()) return;

        if (isEditMode) {
            BookUpdateRequest request = new BookUpdateRequest();
            request.setTitle(getText(binding.inputTitle));
            request.setAuthor(getText(binding.inputAuthor));
            request.setPublisher(getText(binding.inputPublisher));
            request.setPubDate(getText(binding.inputPubDate));
            request.setTotalCopies(getInt(binding.inputTotalCopies));
            request.setLocation(getText(binding.inputLocation));
            request.setDescription(getText(binding.inputDescription));
            request.setKeywords(getText(binding.inputKeywords));
            request.setCategoryId(categoryId);
            viewModel.updateBook(editBookId, request);
        } else {
            BookCreateRequest request = new BookCreateRequest();
            request.setIsbn(getText(binding.inputIsbn));
            request.setTitle(getText(binding.inputTitle));
            request.setAuthor(getText(binding.inputAuthor));
            request.setPublisher(getText(binding.inputPublisher));
            request.setPubDate(getText(binding.inputPubDate));
            request.setTotalCopies(getInt(binding.inputTotalCopies));
            request.setLocation(getText(binding.inputLocation));
            request.setDescription(getText(binding.inputDescription));
            request.setKeywords(getText(binding.inputKeywords));
            request.setCategoryId(categoryId);
            viewModel.createBook(request);
        }
    }

    private boolean validateInputs() {
        if (TextUtils.isEmpty(getText(binding.inputTitle))) {
            binding.inputTitle.setError(getString(R.string.validation_title_required));
            return false;
        }
        if (TextUtils.isEmpty(getText(binding.inputAuthor))) {
            binding.inputAuthor.setError(getString(R.string.validation_author_required));
            return false;
        }
        if (!isEditMode && TextUtils.isEmpty(getText(binding.inputIsbn))) {
            binding.inputIsbn.setError(getString(R.string.validation_isbn_required));
            return false;
        }
        return true;
    }

    private void confirmDelete() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_book_message)
                .setPositiveButton(R.string.confirm, (d, w) -> viewModel.deleteBook(editBookId))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void observeViewModel() {
        viewModel.getCreatedBook().observe(this, book -> {
            if (book != null) {
                Toast.makeText(this, R.string.book_created, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getUpdatedBook().observe(this, book -> {
            if (book != null) {
                Toast.makeText(this, R.string.book_updated, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getDeleteResult().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) {
                Toast.makeText(this, R.string.book_deleted, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getText(TextInputLayout layout) {
        return layout.getEditText() != null
                ? layout.getEditText().getText().toString().trim() : "";
    }

    private int getInt(TextInputLayout layout) {
        try {
            return Integer.parseInt(getText(layout));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}