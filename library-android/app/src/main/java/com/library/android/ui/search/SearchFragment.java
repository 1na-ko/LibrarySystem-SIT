package com.library.android.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.library.android.R;
import com.library.android.databinding.FragmentSearchBinding;
import com.library.android.model.BookVO;
import com.library.android.model.CategoryVO;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.PagingScrollListener;
import com.library.android.viewmodel.SearchViewModel;

import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 图书搜索 Fragment — 首页 + 搜索 + 结果分页.
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
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        setupAdapters();
        setupSearchBar();
        setupObservers();

        viewModel.loadHomeData();
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
            Bundle bundle = new Bundle();
            bundle.putLong("categoryId", category.getId());
            bundle.putString("categoryName", category.getName());
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_searchFragment_to_bookDetailFragment, bundle);
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
                viewModel.search(text);
            }
        });
        binding.rvSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSuggestions.setAdapter(suggestionAdapter);

        // 高级搜索点击
        binding.tvAdvancedSearch.setOnClickListener(v ->
                Navigation.findNavController(requireView())
                        .navigate(R.id.action_searchFragment_to_advancedSearchFragment));

        // 分类浏览点击
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
                    binding.rvSuggestions.setVisibility(View.GONE);
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
            if (books != null && !books.isEmpty()) {
                binding.scrollHome.setVisibility(View.GONE);
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
            if (suggestions != null && !suggestions.isEmpty()) {
                suggestionAdapter.submitList(suggestions);
                binding.rvSuggestions.setVisibility(View.VISIBLE);
            } else {
                binding.rvSuggestions.setVisibility(View.GONE);
            }
        });

        viewModel.getTotalResults().observe(getViewLifecycleOwner(), total ->
                binding.tvResultInfo.setText("找到 " + total + " 条结果"));

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            if (!loading) {
                pagingScrollListener.setLoading(false);
            }
        });

        viewModel.hasMore().observe(getViewLifecycleOwner(), hasMore ->
                pagingScrollListener.setHasMore(hasMore));
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
            binding.tvPublisher.setText(item.getPublisher());
            binding.tvAvailCopies.setText("可借 " + item.getAvailCopies() + "/" + item.getTotalCopies());
            binding.tvBorrowCount.setText(item.getBorrowCount() + " 人借过");
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