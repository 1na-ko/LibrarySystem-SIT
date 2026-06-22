package com.library.android.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.library.android.R;
import com.library.android.databinding.FragmentCategoryTreeBinding;
import com.library.android.model.CategoryVO;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.common.LoadingState;
import com.library.android.ui.common.NavArgKeys;
import com.library.android.ui.main.MainActivity;
import com.library.android.viewmodel.CategoryTreeViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 分类浏览 Fragment — 树形结构.
 *
 * <p>P1-01：移除 {@code @Inject BookRepository}，改由 {@link CategoryTreeViewModel}
 * 加载分类树，UI 通过 LiveData + LoadingState + errorEvent 三路观察.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class CategoryTreeFragment extends BaseFragment {

    private FragmentCategoryTreeBinding binding;
    private CategoryTreeViewModel viewModel;
    private CategoryTreeAdapter adapter;

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
        viewModel = new ViewModelProvider(this).get(CategoryTreeViewModel.class);

        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.page_title_category_tree));
        }

        // WP-7：点击分类 → 跳搜索页按 categoryId 搜书
        adapter = new CategoryTreeAdapter(category -> {
            Bundle args = new Bundle();
            args.putLong(NavArgKeys.CATEGORY_ID, category.getId());
            args.putString(NavArgKeys.CATEGORY_NAME, category.getName() != null ? category.getName() : "");
            androidx.navigation.Navigation.findNavController(view).navigate(R.id.searchFragment, args);
        });
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCategories.setAdapter(adapter);

        binding.btnRetry.setOnClickListener(v -> {
            binding.layoutError.setVisibility(View.GONE);
            viewModel.loadCategories();
        });

        // P1-01：观察 ViewModel 状态（取代原 BookRepository 内联订阅）
        viewModel.getLoadingState().observe(getViewLifecycleOwner(), this::renderState);
        viewModel.getCategories().observe(getViewLifecycleOwner(), this::renderCategories);
        observeError(viewModel.getErrorEvent());

        if (viewModel.getCategories().getValue() == null) {
            viewModel.loadCategories();
        }
    }

    private void renderState(@Nullable LoadingState state) {
        if (binding == null || state == null) return;
        switch (state) {
            case LOADING:
                binding.layoutLoading.setVisibility(View.VISIBLE);
                binding.layoutContent.setVisibility(View.GONE);
                binding.layoutError.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
                break;
            case CONTENT:
                binding.layoutLoading.setVisibility(View.GONE);
                binding.layoutContent.setVisibility(View.VISIBLE);
                binding.layoutError.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
                break;
            case EMPTY:
                binding.layoutLoading.setVisibility(View.GONE);
                binding.layoutContent.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                binding.layoutError.setVisibility(View.GONE);
                break;
            case ERROR:
                binding.layoutLoading.setVisibility(View.GONE);
                binding.layoutContent.setVisibility(View.GONE);
                binding.layoutEmpty.setVisibility(View.GONE);
                binding.layoutError.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void renderCategories(@Nullable List<CategoryVO> tree) {
        if (binding == null) return;
        if (tree == null) return;
        adapter.submitList(flattenTree(tree, 0));
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
