package com.library.android.ui.search;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.library.android.R;
import com.library.android.databinding.FragmentSearchBinding;
import com.library.android.model.BookVO;
import com.library.android.model.CategoryVO;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.PagingScrollListener;
import com.library.android.ui.main.MainActivity;
import com.library.android.viewmodel.SearchViewModel;

import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 图书搜索 Fragment — 首页 + 搜索 + 结果分页.
 * v2.1: 搜索图标移至全局顶部栏（activity_main.xml），
 *       点击后展开搜索输入区域并弹出键盘.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;

    private BookAdapter searchResultAdapter;
    private BookAdapter hotBookAdapter;
    private CategoryAdapter categoryAdapter;
    private SuggestionAdapter suggestionAdapter;
    private PagingScrollListener pagingScrollListener;

    /** 搜索输入区域是否展开 */
    private boolean searchExpanded = false;

    /** 分类导航是否展开 */
    private boolean isCategoryExpanded = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // CRITICAL: Reset state to prevent stale text from leaking on tab switch
        searchExpanded = false;
        binding.layoutSearchInput.setVisibility(View.GONE);

        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        setupAdapters();
        setupSearchBar();
        setupObservers();

        // 检查是否需自动展开搜索（由全局搜索图标触发）
        Bundle args = getArguments();
        if (args != null && args.getBoolean("expandSearch", false)) {
            expandSearch();
            // 清除标记，避免旋转屏幕时再次触发
            args.remove("expandSearch");
        }

        if (args != null && (args.containsKey("title") || args.containsKey("author")
                || args.containsKey("isbn") || args.containsKey("publisher"))) {
            handleAdvancedSearchArgs(args);
        } else if (!searchExpanded) {
            viewModel.loadHomeData();
            addHotSearchChips();
        }
    }

    /**
     * 由 MainActivity 全局搜索图标调用 —— 展开搜索栏并弹出键盘.
     * 若此 Fragment 尚未创建，会通过 arguments 延迟到 onViewCreated 展开.
     */
    public void expandSearchFromGlobal() {
        if (binding != null && isAdded()) {
            expandSearch();
        } else {
            Bundle args = getArguments() != null ? getArguments() : new Bundle();
            args.putBoolean("expandSearch", true);
            setArguments(args);
        }
    }

    /** 展开搜索栏并弹出键盘. */
    private void expandSearch() {
        if (binding == null) return;
        searchExpanded = true;
        binding.layoutSearchInput.setVisibility(View.VISIBLE);
        binding.etSearch.requestFocus();

        binding.etSearch.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT);
        }, 150);
    }

    /** 收起搜索栏并隐藏键盘. */
    private void collapseSearch() {
        if (binding == null) return;
        searchExpanded = false;
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);

        binding.etSearch.setText("");
        binding.rvSuggestions.setVisibility(View.GONE);
        binding.layoutSearchInput.setVisibility(View.GONE);
    }

    /**
     * 处理返回键：展开状态下收起搜索；搜索结果页中返回首页；
     * 否则由 Activity 处理.
     */
    public boolean onBackPressed() {
        if (searchExpanded) {
            collapseSearch();
            if (binding.layoutSearchResults.getVisibility() == View.VISIBLE) {
                backToHome();
            }
            return true;
        }
        if (binding.layoutSearchResults.getVisibility() == View.VISIBLE
                && binding.scrollHome.getVisibility() != View.VISIBLE) {
            backToHome();
            return true;
        }
        return false;
    }

    /** 控制全局返回按钮（通过 MainActivity）. */
    private void setGlobalBack(boolean visible) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setGlobalBackVisible(visible);
        }
    }

    /** 隐藏首页内容（热门搜索、分类导航、热门图书）. */
    private void hideHomeContent() {
        binding.scrollHome.setVisibility(View.GONE);
        binding.chipGroupHotTags.removeAllViews();
        binding.rvSuggestions.setVisibility(View.GONE);
    }

    /** 返回首页视图. */
    private void backToHome() {
        setGlobalBack(false);
        binding.scrollHome.setVisibility(View.VISIBLE);
        binding.layoutSearchResults.setVisibility(View.GONE);
        binding.layoutResultHeader.setVisibility(View.GONE);
        binding.etSearch.setText("");
        binding.rvSuggestions.setVisibility(View.GONE);
        binding.tvResultInfo.setText("");
        viewModel.clearSearchState();
        viewModel.loadHomeData();
        addHotSearchChips();
    }

    private void handleAdvancedSearchArgs(Bundle args) {
        String title = args.getString("title");
        String author = args.getString("author");
        String isbn = args.getString("isbn");
        String publisher = args.getString("publisher");
        Integer pubYearFrom = args.containsKey("pubYearFrom") ? args.getInt("pubYearFrom") : null;
        Integer pubYearTo = args.containsKey("pubYearTo") ? args.getInt("pubYearTo") : null;
        Boolean onlyAvailable = args.containsKey("onlyAvailable") ? args.getBoolean("onlyAvailable") : null;

        binding.layoutSearchInput.setVisibility(View.VISIBLE);
        searchExpanded = true;
        hideHomeContent();
        binding.layoutSearchResults.setVisibility(View.VISIBLE);
        binding.layoutResultHeader.setVisibility(View.GONE);
        setGlobalBack(true);

        viewModel.searchAdvanced(title, author, isbn, publisher, pubYearFrom, pubYearTo, onlyAvailable);
    }

    private void setupAdapters() {
        // 搜索结果适配器
        searchResultAdapter = new BookAdapter(book -> {
            Bundle bundle = new Bundle();
            bundle.putLong("bookId", book.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_searchFragment_to_bookDetailFragment, bundle);
        });

        LinearLayoutManager resultLayoutManager = new LinearLayoutManager(requireContext());
        binding.rvSearchResults.setLayoutManager(resultLayoutManager);
        binding.rvSearchResults.setAdapter(searchResultAdapter);

        pagingScrollListener = new PagingScrollListener(resultLayoutManager) {
            @Override
            protected void loadMore() {
                viewModel.loadMore();
            }
        };
        binding.rvSearchResults.addOnScrollListener(pagingScrollListener);

        // 热门图书适配器
        hotBookAdapter = new BookAdapter(book -> {
            Bundle bundle = new Bundle();
            bundle.putLong("bookId", book.getId());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_searchFragment_to_bookDetailFragment, bundle);
        });
        binding.rvHotBooks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHotBooks.setAdapter(hotBookAdapter);

        // 分类导航适配器
        categoryAdapter = new CategoryAdapter(category -> {
            hideHomeContent();
            binding.layoutSearchResults.setVisibility(View.VISIBLE);
            binding.layoutResultHeader.setVisibility(View.VISIBLE);
            binding.tvResultTitle.setText(category.getName());
            setGlobalBack(true);
            viewModel.searchByCategory(category.getId(), category.getName());
        });
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCategories.setAdapter(categoryAdapter);

        // 搜索建议适配器
        suggestionAdapter = new SuggestionAdapter(suggestion -> {
            String text = suggestion.get("value");
            if (text != null) {
                binding.etSearch.setText(text);
                binding.etSearch.setSelection(text.length());
                binding.rvSuggestions.setVisibility(View.GONE);
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
                setGlobalBack(true);
                viewModel.search(text);
            }
        });
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSuggestions.setAdapter(suggestionAdapter);

        // 高级搜索点击
        binding.tvAdvancedSearch.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_searchFragment_to_advancedSearchFragment));

        // 分类导航可折叠标题点击
        binding.tvCategoryHeader.setOnClickListener(v -> {
            isCategoryExpanded = !isCategoryExpanded;
            if (isCategoryExpanded) {
                binding.rvCategories.setVisibility(View.VISIBLE);
                binding.tvCategoryHeader.setText("分类导航 ▾");
            } else {
                binding.rvCategories.setVisibility(View.GONE);
                binding.tvCategoryHeader.setText("分类导航 ▸");
            }
        });

        // 分类浏览点击（搜索栏内链接 → 跳转分类树页面）
        binding.tvCategoryBrowse.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_searchFragment_to_categoryTreeFragment));
    }

    private void setupSearchBar() {
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String keyword = binding.etSearch.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    InputMethodManager imm = (InputMethodManager) requireContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
                    hideHomeContent();
                    binding.layoutSearchResults.setVisibility(View.VISIBLE);
                    binding.layoutResultHeader.setVisibility(View.GONE);
                    setGlobalBack(true);
                    viewModel.search(keyword);
                }
                return true;
            }
            return false;
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().trim();
                if (text.length() >= 2) {
                    viewModel.loadSuggestions(text);
                } else {
                    binding.rvSuggestions.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupObservers() {
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), books -> {
            if (books != null) {
                hideHomeContent();
                binding.layoutSearchResults.setVisibility(View.VISIBLE);
                searchResultAdapter.submitList(books);
                pagingScrollListener.setLoading(false);
            }
        });

        viewModel.getHotBooks().observe(getViewLifecycleOwner(), books -> {
            if (books != null) {
                hotBookAdapter.submitList(books);
            }
        });

        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) {
                categoryAdapter.submitList(categories);
            }
        });

        viewModel.getSuggestions().observe(getViewLifecycleOwner(), suggestions -> {
            // 搜索结果可见时禁止显示建议词（防止建议 API 后返回覆盖搜索结果）
            if (binding.layoutSearchResults.getVisibility() == View.VISIBLE) {
                binding.rvSuggestions.setVisibility(View.GONE);
                return;
            }
            if (suggestions != null && !suggestions.isEmpty()) {
                suggestionAdapter.submitList(suggestions);
                binding.rvSuggestions.setVisibility(View.VISIBLE);
            } else {
                binding.rvSuggestions.setVisibility(View.GONE);
            }
        });

        viewModel.getTotalResults().observe(getViewLifecycleOwner(), total ->
                binding.tvResultInfo.setText("找到 " + total + " 条结果"));

        // 观察结果标题：Fragment 重建时自动恢复头部返回按钮
        viewModel.getResultTitle().observe(getViewLifecycleOwner(), title -> {
            if (title != null && !title.isEmpty()) {
                hideHomeContent();
                binding.layoutSearchResults.setVisibility(View.VISIBLE);
                binding.layoutResultHeader.setVisibility(View.VISIBLE);
                binding.tvResultTitle.setText(title);
                setGlobalBack(true);
            } else {
                binding.layoutResultHeader.setVisibility(View.GONE);
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            if (!loading) {
                pagingScrollListener.setLoading(false);
            }
        });

        viewModel.hasMore().observe(getViewLifecycleOwner(), hasMore ->
                pagingScrollListener.setHasMore(hasMore));
    }

    /**
     * 填充热门搜索标签（ChipGroup），每项点击后填入搜索框并执行搜索。
     */
    private void addHotSearchChips() {
        if (binding == null) return;
        binding.chipGroupHotTags.removeAllViews();
        String[] hotTags = {"Java", "Python", "机器学习", "数据结构", "人工智能", "数据库", "操作系统", "计算机网络"};
        for (String tag : hotTags) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag);
            chip.setChipBackgroundColorResource(R.color.bg_card);
            chip.setChipStrokeColorResource(R.color.border_light);
            chip.setChipStrokeWidth(1f);
            chip.setTextColor(getResources().getColor(R.color.text_secondary, null));
            chip.setChipCornerRadiusResource(R.dimen.radius_sm);
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setOnClickListener(v -> {
                expandSearch();
                binding.etSearch.setText(tag);
                binding.etSearch.setSelection(tag.length());
                binding.rvSuggestions.setVisibility(View.GONE);
                setGlobalBack(true);
                viewModel.search(tag);
            });
            binding.chipGroupHotTags.addView(chip);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ======================== Book Adapter ========================

    private static class BookAdapter extends BaseAdapter<BookVO, com.library.android.databinding.ItemBookBinding> {

        private final OnBookClickListener listener;

        interface OnBookClickListener {
            void onClick(BookVO book);
        }

        BookAdapter(OnBookClickListener listener) {
            super(R.layout.item_book, new DiffUtil.ItemCallback<BookVO>() {
                @Override
                public boolean areItemsTheSame(@NonNull BookVO oldItem, @NonNull BookVO newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull BookVO oldItem, @NonNull BookVO newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle())
                            && oldItem.getAvailCopies() == newItem.getAvailCopies();
                }
            });
            this.listener = listener;
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
            binding.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }

    // ======================== Category Adapter ========================

    private static class CategoryAdapter extends BaseAdapter<CategoryVO, com.library.android.databinding.ItemCategoryBinding> {

        private final OnCategoryClickListener listener;

        interface OnCategoryClickListener {
            void onClick(CategoryVO category);
        }

        CategoryAdapter(OnCategoryClickListener listener) {
            super(R.layout.item_category, new DiffUtil.ItemCallback<CategoryVO>() {
                @Override
                public boolean areItemsTheSame(@NonNull CategoryVO oldItem, @NonNull CategoryVO newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull CategoryVO oldItem, @NonNull CategoryVO newItem) {
                    return oldItem.getName().equals(newItem.getName());
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
            binding.tvCategoryName.setText(item.getName());
            if (item.hasChildren()) {
                binding.ivExpand.setVisibility(View.VISIBLE);
            } else {
                binding.ivExpand.setVisibility(View.GONE);
            }
            binding.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }

    // ======================== Suggestion Adapter ========================

    private static class SuggestionAdapter extends BaseAdapter<Map<String, String>, com.library.android.databinding.ItemSuggestionBinding> {

        private final OnSuggestionClickListener listener;

        interface OnSuggestionClickListener {
            void onClick(Map<String, String> suggestion);
        }

        SuggestionAdapter(OnSuggestionClickListener listener) {
            super(R.layout.item_suggestion, new DiffUtil.ItemCallback<Map<String, String>>() {
                @Override
                public boolean areItemsTheSame(@NonNull Map<String, String> oldItem, @NonNull Map<String, String> newItem) {
                    return oldItem.get("value").equals(newItem.get("value"));
                }

                @Override
                public boolean areContentsTheSame(@NonNull Map<String, String> oldItem, @NonNull Map<String, String> newItem) {
                    return true;
                }
            });
            this.listener = listener;
        }

        @Override
        protected com.library.android.databinding.ItemSuggestionBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return com.library.android.databinding.ItemSuggestionBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(com.library.android.databinding.ItemSuggestionBinding binding, Map<String, String> item, int position) {
            binding.tvSuggestion.setText(item.get("value"));
            binding.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }
}
