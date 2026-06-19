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
import com.library.android.databinding.FragmentCategoryTreeBinding;
import com.library.android.model.CategoryVO;
import com.library.android.repository.BookRepository;
import com.library.android.ui.main.MainActivity;
import com.library.android.ui.common.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 分类浏览 Fragment — 树形结构.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class CategoryTreeFragment extends Fragment {

    private FragmentCategoryTreeBinding binding;
    private CategoryTreeAdapter adapter;
    private List<CategoryVO> allCategories = new ArrayList<>();

    @Inject
    BookRepository bookRepository;

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoryTreeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.page_title_category_tree));

        // WP-7：点击分类 → 跳搜索页按 categoryId 搜书
        adapter = new CategoryTreeAdapter(category -> {
            Bundle args = new Bundle();
            args.putLong("categoryId", category.getId());
            args.putString("categoryName", category.getName() != null ? category.getName() : "");
            androidx.navigation.Navigation.findNavController(view).navigate(R.id.searchFragment, args);
        });
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCategories.setAdapter(adapter);

        loadCategories();
    }

    private void loadCategories() {
        binding.layoutLoading.setVisibility(View.VISIBLE);
        binding.layoutContent.setVisibility(View.GONE);

        disposables.add(
            bookRepository.getCategoryTree()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    result -> {
                        if (binding == null) return;
                        binding.layoutLoading.setVisibility(View.GONE);
                        if (result.isSuccess() && result.getData() != null) {
                            allCategories = result.getData();
                            adapter.submitList(flattenTree(allCategories, 0));
                            binding.layoutContent.setVisibility(View.VISIBLE);
                        } else {
                            binding.layoutEmpty.setVisibility(View.VISIBLE);
                        }
                    },
                    throwable -> {
                        if (binding == null) return;
                        binding.layoutLoading.setVisibility(View.GONE);
                        binding.layoutError.setVisibility(View.VISIBLE);
                        binding.tvError.setText("加载失败：" + throwable.getMessage());
                    }
                )
        );

        binding.btnRetry.setOnClickListener(v -> {
            binding.layoutError.setVisibility(View.GONE);
            loadCategories();
        });
    }

    /** 将树形分类展平为带缩进的列表. */
    private List<CategoryVO> flattenTree(List<CategoryVO> nodes, int level) {
        List<CategoryVO> result = new ArrayList<>();
        for (CategoryVO node : nodes) {
            result.add(node);
            if (node.hasChildren()) {
                result.addAll(flattenTree(node.getChildren(), level + 1));
            }
        }
        return result;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }

    // ======================== CategoryTreeAdapter ========================

    private static class CategoryTreeAdapter extends BaseAdapter<CategoryVO, com.library.android.databinding.ItemCategoryBinding> {

        interface OnCategoryClick { void onClick(CategoryVO category); }
        private final OnCategoryClick listener;

        CategoryTreeAdapter(OnCategoryClick listener) {
            super(R.layout.item_category, new DiffUtil.ItemCallback<CategoryVO>() {
                @Override
                public boolean areItemsTheSame(@NonNull CategoryVO oldItem, @NonNull CategoryVO newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull CategoryVO oldItem, @NonNull CategoryVO newItem) {
                    return java.util.Objects.equals(oldItem.getName(), newItem.getName());
                }
            });
            this.listener = listener;
        }

        @Override
        protected com.library.android.databinding.ItemCategoryBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return com.library.android.databinding.ItemCategoryBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(com.library.android.databinding.ItemCategoryBinding binding, CategoryVO item, int position) {
            // WP-7：null 兜底
            binding.tvCategoryName.setText(item.getName() != null ? item.getName() : "");
            if (item.hasChildren()) {
                binding.ivExpand.setVisibility(View.VISIBLE);
                binding.ivExpand.setImageResource(android.R.drawable.arrow_up_float);
            } else {
                binding.ivExpand.setVisibility(View.GONE);
            }
            // WP-7 P0 死按钮修复：点击分类 → 跳搜索页按分类搜书
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onClick(item);
            });
        }
    }
}