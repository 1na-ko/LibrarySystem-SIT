package com.library.android.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.library.android.R;
import com.library.android.databinding.FragmentHotBooksBinding;
import com.library.android.model.BookVO;
import com.library.android.model.CategoryVO;
import com.library.android.repository.BookRepository;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.main.MainActivity;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 热门图书排行榜 Fragment.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class HotBooksFragment extends Fragment {

    private FragmentHotBooksBinding binding;
    private BookAdapter adapter;
    private Long selectedCategoryId;

    @Inject
    BookRepository bookRepository;

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHotBooksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((MainActivity) requireActivity()).setGlobalTitle("热门图书");

        adapter = new BookAdapter();
        binding.rvHotBooks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHotBooks.setAdapter(adapter);

        loadCategories();
        loadHotBooks(null);
    }

    private void loadCategories() {
        disposables.add(
            bookRepository.getCategoryTree()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        if (binding == null) return;
                        if (result.isSuccess() && result.getData() != null) {
                            // 添加"全部"选项
                            Chip allChip = new Chip(requireContext());
                            allChip.setText("全部");
                            allChip.setCheckable(true);
                            allChip.setChecked(true);
                            allChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                if (isChecked) {
                                    selectedCategoryId = null;
                                    loadHotBooks(null);
                                }
                            });
                            binding.chipGroupCategories.addView(allChip);

                            for (CategoryVO category : result.getData()) {
                                Chip chip = new Chip(requireContext());
                                chip.setText(category.getName());
                                chip.setCheckable(true);
                                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                    if (isChecked) {
                                        selectedCategoryId = category.getId();
                                        loadHotBooks(category.getId());
                                    }
                                });
                                binding.chipGroupCategories.addView(chip);
                            }
                        }
                    },
                    throwable -> {
                        if (binding == null) return;
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                    }
                )
        );
    }

    private void loadHotBooks(Long categoryId) {
        binding.layoutEmpty.setVisibility(View.GONE);
        disposables.add(
            bookRepository.getHotBooks(categoryId, 50)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        if (binding == null) return;
                        if (result.isSuccess() && result.getData() != null) {
                            adapter.submitList(result.getData());
                        } else {
                            binding.layoutEmpty.setVisibility(View.VISIBLE);
                        }
                    },
                    throwable -> {
                        if (binding == null) return;
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                    }
                )
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }

    // ======================== BookAdapter ========================

    private static class BookAdapter extends BaseAdapter<BookVO, com.library.android.databinding.ItemBookBinding> {

        BookAdapter() {
            super(R.layout.item_book, new DiffUtil.ItemCallback<BookVO>() {
                @Override
                public boolean areItemsTheSame(@NonNull BookVO oldItem, @NonNull BookVO newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull BookVO oldItem, @NonNull BookVO newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle());
                }
            });
        }

        @Override
        protected com.library.android.databinding.ItemBookBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return com.library.android.databinding.ItemBookBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(com.library.android.databinding.ItemBookBinding binding, BookVO item, int position) {
            binding.tvTitle.setText(item.getTitle());
            binding.tvAuthor.setText(item.getAuthor());
            binding.tvAvailCopies.setText("可借 " + item.getAvailCopies() + "/" + item.getTotalCopies());
        }
    }
}