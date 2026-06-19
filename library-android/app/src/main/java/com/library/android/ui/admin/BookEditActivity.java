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
import com.library.android.model.BookCreateRequest;
import com.library.android.model.BookUpdateRequest;
import com.library.android.model.CategoryVO;
import com.library.android.repository.BookRepository;
import com.library.android.viewmodel.AdminViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 图书编目 Activity — 新增/修改图书.
 *
 * <p>通过 Intent extra "bookId" 区分新增（bookId=-1）和编辑模式.
 * <p>B.8 修复：原硬编码 categoryId=1，新增分类选择器（点击 inputCategory 弹出分类列表）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class BookEditActivity extends AppCompatActivity {

    private ActivityBookEditBinding binding;
    private AdminViewModel viewModel;

    @Inject
    BookRepository bookRepository;

    private final CompositeDisposable disposables = new CompositeDisposable();

    private boolean isEditMode = false;
    private long editBookId = -1;

    /** 选中的分类 ID（-1 表示未选择，提交前必须选择）. */
    private long categoryId = -1;

    /** 展平后的分类列表（id + name），供 AlertDialog 选择器使用. */
    private final List<FlatCategory> flatCategories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // WP3.2：深色主题已通过 AndroidManifest.xml 静态声明，不再硬编码修改全局 ThemeManager 状态
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
        } else {
            setTitle(R.string.add_book_title);
            binding.btnDelete.setVisibility(View.GONE);
        }

        // 分类选择器：点击弹出 AlertDialog
        binding.etCategory.setOnClickListener(v -> openCategoryPicker());

        binding.btnSave.setOnClickListener(v -> saveBook());
        observeViewModel();
        loadCategories();
    }

    /** 加载分类树并展平为 List，供选择器使用. */
    private void loadCategories() {
        disposables.add(bookRepository.getCategoryTree()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        flatCategories.clear();
                        flattenCategories(result.getData(), 0);
                    }
                }, throwable -> Toast.makeText(this, getString(R.string.bookedit_category_load_failed), Toast.LENGTH_SHORT).show()));
    }

    private void flattenCategories(List<CategoryVO> nodes, int depth) {
        if (nodes == null) return;
        String indent = depth == 0 ? "" : new String(new char[depth * 2]).replace('\0', ' ');
        for (CategoryVO node : nodes) {
            flatCategories.add(new FlatCategory(node.getId(), indent + node.getName()));
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                flattenCategories(node.getChildren(), depth + 1);
            }
        }
    }

    private void openCategoryPicker() {
        if (flatCategories.isEmpty()) {
            Toast.makeText(this, getString(R.string.bookedit_category_loading), Toast.LENGTH_SHORT).show();
            loadCategories();
            return;
        }
        String[] names = new String[flatCategories.size()];
        for (int i = 0; i < flatCategories.size(); i++) {
            names[i] = flatCategories.get(i).name;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.category_hint)
                .setItems(names, (d, which) -> {
                    FlatCategory selected = flatCategories.get(which);
                    categoryId = selected.id;
                    if (binding.etCategory != null) {
                        binding.etCategory.setText(selected.name);
                    }
                })
                .show();
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
            if (categoryId > 0) request.setCategoryId(categoryId);
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
        // B.8 修复：新增模式下必须选择分类（原硬编码 1 不合理）
        if (!isEditMode && categoryId <= 0) {
            binding.inputCategory.setError(getString(R.string.bookedit_select_category));
            Toast.makeText(this, getString(R.string.bookedit_select_category), Toast.LENGTH_SHORT).show();
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

        viewModel.getErrorEvent().observe(this, throwable -> {
            if (throwable != null && throwable.getMessage() != null && !throwable.getMessage().isEmpty()) {
                Toast.makeText(this, throwable.getMessage() != null ? throwable.getMessage() : "", Toast.LENGTH_SHORT).show();
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
        disposables.clear();
        binding = null;
    }

    /** 展平后的分类条目（含层级缩进）. */
    private static class FlatCategory {
        final long id;
        final String name;

        FlatCategory(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
