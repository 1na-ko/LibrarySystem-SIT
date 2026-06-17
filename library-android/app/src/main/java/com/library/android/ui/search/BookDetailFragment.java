package com.library.android.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.library.android.R;
import com.library.android.databinding.FragmentBookDetailBinding;
import com.library.android.model.BookDetailVO;
import com.library.android.model.BookRecommendVO;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.viewmodel.BookDetailViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 图书详情 Fragment.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class BookDetailFragment extends Fragment {

    private FragmentBookDetailBinding binding;
    private BookDetailViewModel viewModel;
    private RecommendAdapter recommendAdapter;

    private long bookId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBookDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BookDetailViewModel.class);

        ((com.library.android.ui.main.MainActivity) requireActivity()).setGlobalTitle("图书详情");

        bookId = getArguments() != null ? getArguments().getLong("bookId", 0) : 0;

        // 相关推荐
        recommendAdapter = new RecommendAdapter();
        binding.rvRelatedBooks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRelatedBooks.setAdapter(recommendAdapter);

        setupObservers();

        if (bookId > 0) {
            viewModel.loadBookDetail(bookId);
        }
    }

    private void setupObservers() {
        viewModel.getBookDetail().observe(getViewLifecycleOwner(), detail -> {
            if (detail != null) {
                bindDetail(detail);
            }
        });

        viewModel.getRelatedBooks().observe(getViewLifecycleOwner(), books -> {
            if (books != null) {
                recommendAdapter.submitList(books);
            }
        });

        viewModel.isLoading().observe(getViewLifecycleOwner(), loading -> {
            // loading state handled by textLoading view if needed
        });
    }

    private void bindDetail(BookDetailVO detail) {
        binding.tvTitle.setText(detail.getTitle());
        binding.tvAuthor.setText(detail.getAuthor());
        binding.tvPublisher.setText(detail.getPublisher() + " · " + detail.getPubDate());
        binding.tvIsbn.setText("ISBN: " + detail.getIsbn());
        binding.tvCategory.setText(detail.getCategoryName());
        binding.tvLocation.setText(detail.getLocation());
        binding.tvPubDate.setText(detail.getPubDate());
        binding.tvDescription.setText(detail.getDescription());
        binding.tvAvailCopies.setText("可借 " + detail.getAvailCopies() + " / " + detail.getTotalCopies() + " 册");
        binding.tvReservationCount.setText(detail.getReservationCount() + " 人预约");

        // 库存为 0 时显示预约按钮
        if (detail.getAvailCopies() <= 0) {
            binding.btnReserve.setVisibility(View.VISIBLE);
            binding.btnBorrow.setVisibility(View.GONE);
        } else {
            binding.btnReserve.setVisibility(View.GONE);
            binding.btnBorrow.setVisibility(View.VISIBLE);
        }

        // 关键词标签
        if (detail.getKeywords() != null) {
            binding.chipGroupKeywords.removeAllViews();
            for (String keyword : detail.getKeywords()) {
                Chip chip = new Chip(requireContext());
                chip.setText(keyword);
                chip.setClickable(false);
                chip.setCheckable(false);
                binding.chipGroupKeywords.addView(chip);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ======================== Recommend Adapter ========================

    private static class RecommendAdapter extends BaseAdapter<BookRecommendVO, com.library.android.databinding.ItemRecommendationBinding> {

        RecommendAdapter() {
            super(R.layout.item_recommendation, new DiffUtil.ItemCallback<BookRecommendVO>() {
                @Override
                public boolean areItemsTheSame(@NonNull BookRecommendVO oldItem, @NonNull BookRecommendVO newItem) {
                    return oldItem.getBook().getId() == newItem.getBook().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull BookRecommendVO oldItem, @NonNull BookRecommendVO newItem) {
                    return oldItem.getScore() == newItem.getScore();
                }
            });
        }

        @Override
        protected com.library.android.databinding.ItemRecommendationBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return com.library.android.databinding.ItemRecommendationBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(com.library.android.databinding.ItemRecommendationBinding binding, BookRecommendVO item, int position) {
            binding.tvTitle.setText(item.getBook().getTitle());
            binding.tvAuthor.setText(item.getBook().getAuthor());
            binding.tvReason.setText(item.getReason());
        }
    }
}