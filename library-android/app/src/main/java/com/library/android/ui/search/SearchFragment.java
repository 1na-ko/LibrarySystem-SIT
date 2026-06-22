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
import androidx.lifecycle.ViewModelProvider;

import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.common.NavArgKeys;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.library.android.R;
import com.library.android.databinding.FragmentSearchBinding;
import com.library.android.model.BookSimpleVO;
import com.library.android.model.CategoryVO;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.viewmodel.SearchViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 图书搜索首页 — WP-14 重构：优雅搜索入口 + Tab 切换热门/分类，搜索结果独立页面.
 *
 * <p>搜索框（悬浮圆角卡片）+ TabLayout（热门搜索 / 分类浏览）→ 提交搜索后导航到 SearchResultsFragment.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class SearchFragment extends BaseFragment {

    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private CategoryAdapter categoryAdapter;
    private SuggestionAdapter suggestionAdapter;

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
        setupToolbar(view, R.string.search);
        // WP-14：必须用 Activity scope，与 SearchResultsFragment 共享同一 ViewModel 实例
        viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);

        setupTabLayout();
        setupCategoryAdapter();
        setupSuggestionAdapter();
        setupSearchBar();
        setupObservers();

        // 接收高级搜索/扫码/分类导航参数 → 立即执行搜索并跳到结果页
        Bundle args = getArguments();
        android.content.Intent activityIntent = requireActivity().getIntent();
        if (args != null && (args.containsKey(NavArgKeys.TITLE) || args.containsKey(NavArgKeys.AUTHOR)
                || args.containsKey(NavArgKeys.ISBN) || args.containsKey(NavArgKeys.PUBLISHER))) {
            handleAdvancedSearchArgs(args);
        } else if (activityIntent != null && activityIntent.hasExtra(NavArgKeys.ISBN)) {
            handleAdvancedSearchArgs(activityIntent.getExtras());
            // 清除 Intent extra 防止 navigateUp 返回后重新触发导航（修复 ISBN 扫码后退死循环）
            activityIntent.removeExtra(NavArgKeys.ISBN);
        } else if (activityIntent != null && activityIntent.hasExtra(NavArgKeys.CATEGORY_ID)) {
            long categoryId = activityIntent.getLongExtra(NavArgKeys.CATEGORY_ID, 0);
            String categoryName = activityIntent.getStringExtra(NavArgKeys.CATEGORY_NAME);
            if (categoryName != null) binding.etSearch.setText(categoryName);
            viewModel.searchByCategory(categoryId, categoryName);
            Navigation.findNavController(view)
                    .navigate(R.id.action_searchFragment_to_searchResultsFragment);
            activityIntent.removeExtra(NavArgKeys.CATEGORY_ID);
            activityIntent.removeExtra(NavArgKeys.CATEGORY_NAME);
        } else {
            viewModel.loadHomeData();
            addHotSearchChips();
        }
    }

    // ======================== Tab 切换 ========================

    private void setupTabLayout() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.hot_search));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.category_nav));
        binding.tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    binding.layoutHotSearch.setVisibility(View.VISIBLE);
                    binding.layoutCategoryBrowse.setVisibility(View.GONE);
                } else {
                    binding.layoutHotSearch.setVisibility(View.GONE);
                    binding.layoutCategoryBrowse.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });
    }

    // ======================== Adapters ========================

    private void setupCategoryAdapter() {
        categoryAdapter = new CategoryAdapter(cat -> {
            viewModel.searchByCategory(cat.getId(), cat.getName());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_searchFragment_to_searchResultsFragment);
        });
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvCategories.setAdapter(categoryAdapter);
    }

    private void setupSuggestionAdapter() {
        suggestionAdapter = new SuggestionAdapter(suggestion -> {
            String text = suggestion.getText();
            if (text != null) {
                binding.etSearch.setText(text);
                binding.etSearch.setSelection(text.length());
                binding.rvSuggestions.setVisibility(View.GONE);
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
                viewModel.search(text);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_searchFragment_to_searchResultsFragment);
            }
        });
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSuggestions.setAdapter(suggestionAdapter);
    }

    // ======================== Search ========================

    private void setupSearchBar() {
        // 回车搜索
        binding.etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                doSearch();
                return true;
            }
            return false;
        });

        // 输入建议 + 清空按钮
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString().trim();
                binding.ivClear.setVisibility(text.length() > 0 ? View.VISIBLE : View.GONE);
                if (text.length() >= 2) viewModel.loadSuggestions(text);
                else binding.rvSuggestions.setVisibility(View.GONE);
            }
        });
        binding.ivClear.setOnClickListener(v -> {
            binding.etSearch.setText("");
            binding.rvSuggestions.setVisibility(View.GONE);
        });

        // 高级搜索 BottomSheet
        binding.btnAdvanced.setOnClickListener(v ->
                new AdvancedSearchFragment().show(getChildFragmentManager(), AdvancedSearchFragment.TAG));

        // 接收高级搜索返回
        getChildFragmentManager().setFragmentResultListener(
                AdvancedSearchFragment.RESULT_KEY, getViewLifecycleOwner(), (reqKey, bundle) -> {
                    handleAdvancedSearchArgs(bundle);
                });
    }

    private void doSearch() {
        if (binding == null) return;
        String keyword = binding.etSearch.getText().toString().trim();
        if (keyword.isEmpty()) return;
        InputMethodManager imm = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.etSearch.getWindowToken(), 0);
        binding.rvSuggestions.setVisibility(View.GONE);
        viewModel.search(keyword);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_searchFragment_to_searchResultsFragment);
    }

    private void handleAdvancedSearchArgs(Bundle args) {
        String title = args.getString(NavArgKeys.TITLE);
        String author = args.getString(NavArgKeys.AUTHOR);
        String isbn = args.getString(NavArgKeys.ISBN);
        String publisher = args.getString(NavArgKeys.PUBLISHER);
        Integer pubYearFrom = args.containsKey("pubYearFrom") ? args.getInt("pubYearFrom") : null;
        Integer pubYearTo = args.containsKey("pubYearTo") ? args.getInt("pubYearTo") : null;
        Boolean onlyAvailable = args.containsKey("onlyAvailable") ? args.getBoolean("onlyAvailable") : null;
        viewModel.searchAdvanced(title, author, isbn, publisher, pubYearFrom, pubYearTo, onlyAvailable);
        Navigation.findNavController(requireView())
                .navigate(R.id.action_searchFragment_to_searchResultsFragment);
    }

    // ======================== Observers ========================

    private void setupObservers() {
        viewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            if (categories != null) categoryAdapter.submitList(categories);
        });
        viewModel.getSuggestions().observe(getViewLifecycleOwner(), suggestions -> {
            if (suggestions != null && !suggestions.isEmpty()) {
                suggestionAdapter.submitList(suggestions);
                binding.rvSuggestions.setVisibility(View.VISIBLE);
            } else {
                binding.rvSuggestions.setVisibility(View.GONE);
            }
        });
    }

    // ======================== Hot Tags ========================

    private void addHotSearchChips() {
        if (binding == null) return;
        binding.chipGroupHotTags.removeAllViews();
        String[] hotTags = getResources().getStringArray(R.array.hot_search_tags);
        for (String tag : hotTags) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag);
            chip.setChipBackgroundColorResource(R.color.bg_card);
            chip.setChipStrokeColorResource(R.color.border_light);
            chip.setChipStrokeWidth(1f);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            chip.setChipCornerRadiusResource(R.dimen.radius_sm);
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setOnClickListener(v -> {
                binding.etSearch.setText(tag);
                binding.etSearch.setSelection(tag.length());
                binding.rvSuggestions.setVisibility(View.GONE);
                viewModel.search(tag);
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_searchFragment_to_searchResultsFragment);
            });
            binding.chipGroupHotTags.addView(chip);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public boolean onBackPressed() {
        return false; // WP-2：返回直接交给 NavController
    }

    // ======================== Adapters ========================

    private static class CategoryAdapter extends BaseAdapter<CategoryVO, com.library.android.databinding.ItemCategoryBinding> {
        private final OnCategoryClickListener listener;
        interface OnCategoryClickListener { void onClick(CategoryVO category); }

        CategoryAdapter(OnCategoryClickListener listener) {
            super(R.layout.item_category, new DiffUtil.ItemCallback<CategoryVO>() {
                @Override public boolean areItemsTheSame(@NonNull CategoryVO o, @NonNull CategoryVO n) { return o.getId() == n.getId(); }
                @Override public boolean areContentsTheSame(@NonNull CategoryVO o, @NonNull CategoryVO n) { return java.util.Objects.equals(o.getName(), n.getName()); }
            });
            this.listener = listener;
        }
        @Override protected com.library.android.databinding.ItemCategoryBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return com.library.android.databinding.ItemCategoryBinding.inflate(inflater, parent, false);
        }
        @Override protected void bind(com.library.android.databinding.ItemCategoryBinding b, CategoryVO item, int position) {
            b.tvCategoryName.setText(item.getName() != null ? item.getName() : "");
            b.ivExpand.setVisibility(View.GONE);  // WP-14：搜索页分类不显箭头
            b.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }

    private static class SuggestionAdapter extends BaseAdapter<com.library.android.model.SuggestVO, com.library.android.databinding.ItemSuggestionBinding> {
        private final OnSuggestionClickListener listener;
        interface OnSuggestionClickListener { void onClick(com.library.android.model.SuggestVO suggestion); }

        SuggestionAdapter(OnSuggestionClickListener listener) {
            super(R.layout.item_suggestion, new DiffUtil.ItemCallback<com.library.android.model.SuggestVO>() {
                @Override public boolean areItemsTheSame(@NonNull com.library.android.model.SuggestVO o, @NonNull com.library.android.model.SuggestVO n) { return java.util.Objects.equals(o.getText(), n.getText()); }
                @Override public boolean areContentsTheSame(@NonNull com.library.android.model.SuggestVO o, @NonNull com.library.android.model.SuggestVO n) { return java.util.Objects.equals(o.getType(), n.getType()); }
            });
            this.listener = listener;
        }
        @Override protected com.library.android.databinding.ItemSuggestionBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return com.library.android.databinding.ItemSuggestionBinding.inflate(inflater, parent, false);
        }
        @Override protected void bind(com.library.android.databinding.ItemSuggestionBinding b, com.library.android.model.SuggestVO item, int position) {
            b.tvSuggestion.setText(item.getText() != null ? item.getText() : "");
            b.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }

    // ======================== Global entry from MainActivity ========================

    public void expandSearchFromGlobal() {
        if (binding != null && isAdded()) {
            binding.etSearch.requestFocus();
            binding.etSearch.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) requireContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT);
            }, 150);
        }
    }
}
