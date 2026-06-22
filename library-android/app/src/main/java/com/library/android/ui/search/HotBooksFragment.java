package com.library.android.ui.search;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.library.android.R;
import com.library.android.databinding.FragmentHotBooksBinding;
import com.library.android.model.BookSimpleVO;
import com.library.android.model.CategoryVO;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.common.LoadingState;
import com.library.android.ui.common.NavArgKeys;
import com.library.android.ui.main.MainActivity;
import com.library.android.viewmodel.HotBooksViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 热门图书排行榜 Fragment.
 *
 * <p>WP-14：标签改下拉菜单（MaterialAutoComplete + ExposedDropdownMenu），
 * 节省空间、交互优雅（原 ChipGroup+HorizontalScrollView）.
 *
 * <p>P1-01：移除 {@code @Inject BookRepository}，分类与热门图书加载下沉到
 * {@link HotBooksViewModel}，UI 仅观察 LiveData + LoadingState + errorEvent.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class HotBooksFragment extends BaseFragment {

    private FragmentHotBooksBinding binding;
    private HotBooksViewModel viewModel;
    private BookAdapter adapter;
    private final List<CategoryVO> flatCategories = new ArrayList<>();
    @Nullable
    private Long selectedCategoryId;

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
        viewModel = new ViewModelProvider(this).get(HotBooksViewModel.class);

        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.page_title_hot_books));
        }

        adapter = new BookAdapter();
        binding.rvHotBooks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHotBooks.setAdapter(adapter);

        // 观察分类树（首次加载完成后渲染下拉菜单 + 触发热门图书加载）
        viewModel.getCategories().observe(getViewLifecycleOwner(), this::renderCategoryDropdown);
        viewModel.getHotBooks().observe(getViewLifecycleOwner(), books -> {
            if (binding == null) return;
            adapter.submitList(books != null ? books : new ArrayList<>());
        });
        viewModel.getLoadingState().observe(getViewLifecycleOwner(), state -> {
            if (binding == null) return;
            binding.layoutEmpty.setVisibility(state == LoadingState.EMPTY ? View.VISIBLE : View.GONE);
        });
        observeError(viewModel.getErrorEvent());

        if (viewModel.getCategories().getValue() == null) {
            viewModel.loadCategories();
        }
    }

    /** 根据分类树构建下拉菜单 + 默认加载"全部"分类的热门图书. */
    private void renderCategoryDropdown(@Nullable List<CategoryVO> tree) {
        if (binding == null || tree == null) return;
        flatCategories.clear();
        flatten(tree);

        List<String> names = new ArrayList<>();
        names.add(getString(R.string.tab_all));
        for (CategoryVO c : flatCategories) {
            names.add(c.getName() != null ? c.getName() : "");
        }
        binding.etCategory.setAdapter(
                new android.widget.ArrayAdapter<>(
                        requireContext(), android.R.layout.simple_list_item_1, names));
        binding.etCategory.setOnItemClickListener((parent, v1, position, id) -> {
            if (position == 0) {
                selectedCategoryId = null;
            } else if (position >= 1 && position <= flatCategories.size()) {
                selectedCategoryId = flatCategories.get(position - 1).getId();
            }
            viewModel.loadHotBooks(selectedCategoryId);
        });
        binding.etCategory.setText(getString(R.string.tab_all), false);
        // 初始加载全部
        if (viewModel.getHotBooks().getValue() == null) {
            viewModel.loadHotBooks(null);
        }
    }

    private void flatten(List<CategoryVO> nodes) {
        if (nodes == null) return;
        for (CategoryVO node : nodes) {
            flatCategories.add(node);
            if (node.getChildren() != null) flatten(node.getChildren());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
            String coverUrl = item.getCoverUrl();
            if (!TextUtils.isEmpty(coverUrl)) {
                Glide.with(binding.ivCover.getContext())
                        .load(coverUrl)
                        .placeholder(R.drawable.ic_book_placeholder)
                        .error(R.drawable.ic_book_placeholder)
                        .into(binding.ivCover);
            } else {
                binding.ivCover.setImageResource(R.drawable.ic_book_placeholder);
            }
            binding.getRoot().setOnClickListener(v -> {
                androidx.navigation.NavController nav = androidx.navigation.Navigation.findNavController(v);
                Bundle args = new Bundle();
                args.putLong(NavArgKeys.BOOK_ID, item.getId());
                nav.navigate(R.id.bookDetailFragment, args);
            });
        }
    }
}
