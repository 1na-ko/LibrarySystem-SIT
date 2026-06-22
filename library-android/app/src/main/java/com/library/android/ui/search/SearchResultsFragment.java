package com.library.android.ui.search;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.library.android.R;
import com.library.android.databinding.FragmentSearchResultsBinding;
import com.library.android.model.BookSimpleVO;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.common.LoadingState;
import com.library.android.ui.common.NavArgKeys;
import com.library.android.ui.common.PagingScrollListener;
import com.library.android.viewmodel.SearchViewModel;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 搜索结果独立页面（WP-14：与搜索首页分离，有自己的 toolbar + 结果列表）.
 *
 * <p>接收 Bundle：keyword / isbn / title / author 等高级搜索参数，
 * 通过 Activity scope 的 SearchViewModel 执行搜索并展示结果.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class SearchResultsFragment extends BaseFragment {

    private FragmentSearchResultsBinding binding;
    private SearchViewModel viewModel;
    private SearchResultAdapter adapter;
    private PagingScrollListener scrollListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchResultsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SearchViewModel.class);
        setupToolbar(view, R.string.search_results_title);

        adapter = new SearchResultAdapter(book -> {
            Bundle args = new Bundle();
            args.putLong(NavArgKeys.BOOK_ID, book.getId());
            Navigation.findNavController(view).navigate(R.id.bookDetailFragment, args);
        });
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.rvSearchResults.setLayoutManager(layoutManager);
        binding.rvSearchResults.setAdapter(adapter);

        scrollListener = new PagingScrollListener(layoutManager) {
            @Override
            protected void loadMore() { viewModel.loadMore(); }
        };
        binding.rvSearchResults.addOnScrollListener(scrollListener);

        observeError(viewModel.getErrorEvent());

        // 重新搜索（用 ViewModel 已有状态，已在 SearchFragment 中触发了搜索）
        viewModel.getSearchResults().observe(getViewLifecycleOwner(), books -> {
            if (books != null) {
                adapter.submitList(books);
            }
        });

        viewModel.getTotalResults().observe(getViewLifecycleOwner(), total -> {
            if (binding != null) {
                String methodLabel = viewModel.getSearchMethodLabel().getValue();
                String keyword = viewModel.getResultTitle().getValue();
                if (methodLabel != null && keyword != null && !keyword.isEmpty()) {
                    binding.tvResultInfo.setText(getString(R.string.search_result_with_keyword, methodLabel, keyword, total));
                } else if (keyword != null && !keyword.isEmpty()) {
                    binding.tvResultInfo.setText(getString(R.string.search_result_with_keyword, "搜索", keyword, total));
                } else {
                    binding.tvResultInfo.setText(getString(R.string.search_result_count, total));
                }
                binding.tvResultInfo.setVisibility(View.VISIBLE);
                binding.layoutResultHeader.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getLoadingState().observe(getViewLifecycleOwner(), state -> {
            boolean loading = state == LoadingState.LOADING
                    && (adapter.getCurrentList() == null || adapter.getCurrentList().isEmpty());
            binding.textLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
            boolean empty = state == LoadingState.EMPTY;
            binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            if (state == LoadingState.CONTENT) {
                binding.rvSearchResults.setVisibility(View.VISIBLE);
            }
        });

        // 搜索结果已在 SearchViewModel 中，由 SearchFragment 触发
        java.util.List<BookSimpleVO> existing = viewModel.getSearchResults().getValue();
        if (existing != null) {
            adapter.submitList(existing);
            binding.rvSearchResults.setVisibility(View.VISIBLE);
        }
        Integer total = viewModel.getTotalResults().getValue();
        if (total != null && binding != null) {
            binding.tvResultInfo.setText(getString(R.string.search_result_count, total));
            binding.tvResultInfo.setVisibility(View.VISIBLE);
            binding.layoutResultHeader.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (binding != null && scrollListener != null) {
            binding.rvSearchResults.removeOnScrollListener(scrollListener);
        }
        binding = null;
    }

    // ======================== SearchResultAdapter ========================

    private static class SearchResultAdapter extends BaseAdapter<BookSimpleVO, com.library.android.databinding.ItemBookBinding> {

        interface OnBookClick { void onClick(BookSimpleVO book); }
        private final OnBookClick listener;

        SearchResultAdapter(OnBookClick listener) {
            super(R.layout.item_book, new DiffUtil.ItemCallback<BookSimpleVO>() {
                @Override
                public boolean areItemsTheSame(@NonNull BookSimpleVO o, @NonNull BookSimpleVO n) {
                    return o.getId() == n.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull BookSimpleVO o, @NonNull BookSimpleVO n) {
                    return java.util.Objects.equals(o.getTitle(), n.getTitle());
                }
            });
            this.listener = listener;
        }

        @Override
        protected com.library.android.databinding.ItemBookBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return com.library.android.databinding.ItemBookBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(com.library.android.databinding.ItemBookBinding b, BookSimpleVO item, int position) {
            b.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "");
            b.tvAuthor.setText(item.getAuthor() != null ? item.getAuthor() : "");
            b.tvAvailCopies.setText(b.getRoot().getContext().getString(R.string.avail_copies_format, item.getAvailCopies()));
            String coverUrl = item.getCoverUrl();
            if (!TextUtils.isEmpty(coverUrl)) {
                Glide.with(b.ivCover.getContext())
                        .load(coverUrl)
                        .placeholder(R.drawable.ic_book_placeholder)
                        .error(R.drawable.ic_book_placeholder)
                        .into(b.ivCover);
            } else {
                b.ivCover.setImageResource(R.drawable.ic_book_placeholder);
            }
            b.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }
}
