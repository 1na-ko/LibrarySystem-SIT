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

import com.library.android.R;
import com.library.android.databinding.FragmentHotBooksBinding;
import com.library.android.model.BookSimpleVO;
import com.library.android.model.CategoryVO;
import com.library.android.repository.BookRepository;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.main.MainActivity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 热门图书排行榜 Fragment.
 *
 * <p>WP-14：标签改下拉菜单（MaterialAutoComplete + ExposedDropdownMenu），
 * 节省空间、交互优雅（原 ChipGroup+HorizontalScrollView）。
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
    private final List<CategoryVO> flatCategories = new ArrayList<>();

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

        ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.page_title_hot_books));

        adapter = new BookAdapter();
        binding.rvHotBooks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHotBooks.setAdapter(adapter);

        loadCategories();
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
                            flatCategories.clear();
                            flatten(result.getData());

                            // WP-7：下拉菜单项（第一项"全部"+分类列表）
                            List<String> names = new ArrayList<>();
                            names.add(getString(R.string.tab_all));
                            for (CategoryVO c : flatCategories) names.add(c.getName() != null ? c.getName() : "");
                            binding.etCategory.setAdapter(
                                    new android.widget.ArrayAdapter<>(
                                            requireContext(), android.R.layout.simple_list_item_1, names));
                            binding.etCategory.setOnItemClickListener((parent, v1, position, id) -> {
                                if (position == 0) {
                                    selectedCategoryId = null;
                                } else if (position >= 1 && position <= flatCategories.size()) {
                                    selectedCategoryId = flatCategories.get(position - 1).getId();
                                }
                                loadHotBooks(selectedCategoryId);
                            });
                            binding.etCategory.setText(getString(R.string.tab_all), false);
                            // 初始加载全部
                            loadHotBooks(null);
                        }
                    },
                    throwable -> {
                        if (binding != null) binding.layoutEmpty.setVisibility(View.VISIBLE);
                    }
                )
        );
    }

    private void flatten(List<CategoryVO> nodes) {
        if (nodes == null) return;
        for (CategoryVO node : nodes) {
            flatCategories.add(node);
            if (node.getChildren() != null) flatten(node.getChildren());
        }
    }

    private void loadHotBooks(Long categoryId) {
        binding.layoutEmpty.setVisibility(View.GONE);
        adapter.submitList(new ArrayList<>());
        disposables.add(
            bookRepository.getHotBooks(categoryId, 50)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        if (binding == null) return;
                        if (result.isSuccess() && result.getData() != null) {
                            adapter.submitList(result.getData());
                            if (result.getData().isEmpty()) {
                                binding.layoutEmpty.setVisibility(View.VISIBLE);
                            }
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

    private static class BookAdapter extends BaseAdapter<BookSimpleVO, com.library.android.databinding.ItemBookBinding> {

        BookAdapter() {
            super(R.layout.item_book, new DiffUtil.ItemCallback<BookSimpleVO>() {
                @Override
                public boolean areItemsTheSame(@NonNull BookSimpleVO oldItem, @NonNull BookSimpleVO newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull BookSimpleVO oldItem, @NonNull BookSimpleVO newItem) {
                    return java.util.Objects.equals(oldItem.getTitle(), newItem.getTitle());
                }
            });
        }

        @Override
        protected com.library.android.databinding.ItemBookBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return com.library.android.databinding.ItemBookBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(com.library.android.databinding.ItemBookBinding binding, BookSimpleVO item, int position) {
            binding.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "");
            binding.tvAuthor.setText(item.getAuthor() != null ? item.getAuthor() : "");
            binding.tvAvailCopies.setText(binding.getRoot().getContext().getString(R.string.avail_copies_format, item.getAvailCopies()));
            binding.getRoot().setOnClickListener(v -> {
                androidx.navigation.NavController nav = androidx.navigation.Navigation.findNavController(v);
                Bundle args = new Bundle();
                args.putLong("bookId", item.getId());
                nav.navigate(R.id.bookDetailFragment, args);
            });
        }
    }
}
