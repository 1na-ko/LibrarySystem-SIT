package com.library.android.ui.search;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.library.android.R;
import com.library.android.databinding.FragmentBookDetailBinding;
import com.library.android.ui.common.NavArgKeys;
import com.library.android.model.BookDetailVO;
import com.library.android.model.BookRecommendVO;
import com.library.android.model.BookSimpleVO;
import com.library.android.ui.borrow.BorrowConfirmDialog;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.BaseFragment;
import com.library.android.viewmodel.BookDetailViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 图书详情 Fragment.
 *
 * <p>修复点（阶段 A.3）:
 * <ul>
 *   <li>Glide 加载封面图（含占位图与降级 drawable）.</li>
 *   <li>btnBorrow / btnReserve 接入点击监听（原版无监听，按钮形同虚设）.</li>
 *   <li>btnKnowledgeGraph 跳转图谱页.</li>
 *   <li>所有 setText 输入做 null/空字符兜底，避免 TextView 显示字面量 "null".</li>
 * </ul>
 *
 * <p>P1-01：移除 {@code @Inject ReservationRepository}，预约动作通过
 * {@link BookDetailViewModel#reserveBook(long)} 触发，UI 监听 reserveSuccess
 * + reserving + errorEvent 三路 LiveData.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class BookDetailFragment extends BaseFragment {

    private FragmentBookDetailBinding binding;
    private BookDetailViewModel viewModel;
    private RecommendAdapter recommendAdapter;

    private long bookId;
    @Nullable
    private BookDetailVO currentDetail;

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

        if (requireActivity() instanceof com.library.android.ui.main.MainActivity) {
            ((com.library.android.ui.main.MainActivity) requireActivity()).setGlobalTitle(getString(R.string.page_title_book_detail));
        }

        bookId = getArguments() != null ? getArguments().getLong(NavArgKeys.BOOK_ID, 0) : 0;

        // 相关推荐（WP1.5：popUpTo 自身+inclusive 防深栈溢出）
        recommendAdapter = new RecommendAdapter(item -> {
            if (item.getBook() == null) return;
            Bundle args = new Bundle();
            args.putLong(NavArgKeys.BOOK_ID, item.getBook().getId());
            androidx.navigation.NavOptions navOptions = new androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.bookDetailFragment, true)
                    .setLaunchSingleTop(true)
                    .build();
            Navigation.findNavController(requireView())
                    .navigate(R.id.bookDetailFragment, args, navOptions);
        });
        binding.rvRelatedBooks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRelatedBooks.setAdapter(recommendAdapter);

        binding.btnBorrow.setOnClickListener(this::onClickBorrow);
        binding.btnReserve.setOnClickListener(this::onClickReserve);
        binding.btnKnowledgeGraph.setOnClickListener(this::onClickKnowledgeGraph);

        setupObservers();

        // WP-14：借阅结果反馈（success=true 时弹 Snackbar 反馈 + 刷新详情页库存）
        getChildFragmentManager().setFragmentResultListener("borrow_result", getViewLifecycleOwner(),
                (requestKey, result) -> {
                    boolean success = result.getBoolean("success", false);
                    String title = result.getString("title", "");
                    if (success) {
                        Snackbar.make(requireView(),
                                getString(R.string.borrow_success_format, title),
                                Snackbar.LENGTH_LONG).show();
                        if (bookId > 0) viewModel.loadBookDetail(bookId);
                    }
                });

        if (bookId > 0) {
            viewModel.loadBookDetail(bookId);
        }
    }

    private void setupObservers() {
        viewModel.getBookDetail().observe(getViewLifecycleOwner(), detail -> {
            if (detail != null) {
                currentDetail = detail;
                bindDetail(detail);
            }
        });

        viewModel.getRelatedBooks().observe(getViewLifecycleOwner(), books -> {
            if (books != null) {
                recommendAdapter.submitList(books);
            }
        });

        // P1-01：预约成功反馈（取代 BookDetailFragment 原 handleReserveResult）
        viewModel.getReserveSuccess().observe(getViewLifecycleOwner(), reservation -> {
            if (reservation == null || binding == null) return;
            Snackbar.make(binding.getRoot(),
                    getString(R.string.reserve_success_format, reservation.getQueuePosition()),
                    Snackbar.LENGTH_LONG).show();
        });

        // P1-01：预约状态变更（按钮禁用防抖）
        viewModel.isReserving().observe(getViewLifecycleOwner(), inFlight -> {
            if (binding != null) {
                binding.btnReserve.setEnabled(!Boolean.TRUE.equals(inFlight));
            }
        });

        // 错误统一通过 BaseFragment.observeError 展示（按异常类型分类文案）
        observeError(viewModel.getErrorEvent());
    }

    private void bindDetail(BookDetailVO detail) {
        binding.tvTitle.setText(safe(detail.getTitle()));
        binding.tvAuthor.setText(safe(detail.getAuthor()));

        // 出版社 + 出版日期，缺一不显示分隔点
        String publisher = safe(detail.getPublisher());
        String pubDate = safe(detail.getPubDate());
        if (!publisher.isEmpty() && !pubDate.isEmpty()) {
            binding.tvPublisher.setText(publisher + " · " + pubDate);
        } else {
            binding.tvPublisher.setText(publisher.isEmpty() ? pubDate : publisher);
        }

        binding.tvIsbn.setText(getString(R.string.isbn_format, safe(detail.getIsbn())));
        binding.tvCategory.setText(safe(detail.getCategoryName()));
        binding.tvLocation.setText(safe(detail.getLocation()));
        binding.tvPubDate.setText(safe(detail.getPubDate()));
        binding.tvDescription.setText(safe(detail.getDescription()));
        binding.tvAvailCopies.setText(getString(R.string.detail_avail_format,
                detail.getAvailCopies(), detail.getTotalCopies()));
        binding.tvReservationCount.setText(getString(R.string.detail_reservation_format,
                detail.getReservationCount()));

        // 库存为 0 时显示预约按钮，否则显示借阅按钮
        if (detail.getAvailCopies() <= 0) {
            binding.btnReserve.setVisibility(View.VISIBLE);
            binding.btnBorrow.setVisibility(View.GONE);
        } else {
            binding.btnReserve.setVisibility(View.GONE);
            binding.btnBorrow.setVisibility(View.VISIBLE);
        }

        // 封面图（A.3 关键修复：原代码遗漏 Glide 调用导致封面永不显示）
        if (!TextUtils.isEmpty(detail.getCoverUrl())) {
            Glide.with(this)
                    .load(detail.getCoverUrl())
                    .placeholder(R.color.bg_overview)
                    .error(R.color.bg_overview)
                    .into(binding.ivCover);
        } else {
            binding.ivCover.setImageDrawable(null);
            binding.ivCover.setBackgroundResource(R.color.bg_overview);
        }

        // 关键词标签
        binding.chipGroupKeywords.removeAllViews();
        if (detail.getKeywords() != null) {
            for (String keyword : detail.getKeywords()) {
                if (TextUtils.isEmpty(keyword)) continue;
                Chip chip = new Chip(requireContext());
                chip.setText(keyword);
                chip.setClickable(false);
                chip.setCheckable(false);
                binding.chipGroupKeywords.addView(chip);
            }
        }
    }

    private void onClickBorrow(View v) {
        if (currentDetail == null) return;
        v.setEnabled(false);  // 防止快速双击重复弹窗
        v.postDelayed(() -> { if (binding != null) v.setEnabled(true); }, 600);
        BookSimpleVO simple = BookSimpleVO.fromDetail(currentDetail);
        BorrowConfirmDialog.newInstance(simple)
                .show(getChildFragmentManager(), "borrow_confirm");
    }

    private void onClickReserve(View v) {
        if (currentDetail == null) return;
        // P1-01：预约逻辑下沉到 ViewModel；按钮防抖通过 reserving LiveData 控制
        viewModel.reserveBook(currentDetail.getId());
    }

    private void onClickKnowledgeGraph(View v) {
        if (currentDetail == null) return;
        Bundle args = new Bundle();
        args.putLong(NavArgKeys.BOOK_ID, currentDetail.getId());
        Navigation.findNavController(v).navigate(R.id.action_bookDetailFragment_to_knowledgeGraphFragment, args);
    }

    /** 防 null/literal "null"/前后空格的安全文本兜底. */
    private static String safe(@Nullable String s) {
        return s == null ? "" : s.trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ======================== Recommend Adapter ========================

    /** 相关推荐点击监听 — 由 Fragment 注入，避免 Adapter 持有 Fragment 引用. */
    interface OnRecommendClickListener {
        void onClick(BookRecommendVO item);
    }

    private static class RecommendAdapter extends BaseAdapter<BookRecommendVO, com.library.android.databinding.ItemRecommendationBinding> {

        private final OnRecommendClickListener listener;

        RecommendAdapter(OnRecommendClickListener listener) {
            super(R.layout.item_recommendation, new DiffUtil.ItemCallback<BookRecommendVO>() {
                @Override
                public boolean areItemsTheSame(@NonNull BookRecommendVO oldItem, @NonNull BookRecommendVO newItem) {
                    return oldItem.getBook() != null && newItem.getBook() != null
                            && oldItem.getBook().getId() == newItem.getBook().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull BookRecommendVO oldItem, @NonNull BookRecommendVO newItem) {
                    return Double.compare(oldItem.getScore(), newItem.getScore()) == 0;
                }
            });
            this.listener = listener;
        }

        @Override
        protected com.library.android.databinding.ItemRecommendationBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return com.library.android.databinding.ItemRecommendationBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(com.library.android.databinding.ItemRecommendationBinding binding, BookRecommendVO item, int position) {
            if (item.getBook() != null) {
                binding.tvTitle.setText(item.getBook().getTitle() != null ? item.getBook().getTitle() : "");
                binding.tvAuthor.setText(item.getBook().getAuthor() != null ? item.getBook().getAuthor() : "");
                String coverUrl = item.getBook().getCoverUrl();
                if (!TextUtils.isEmpty(coverUrl)) {
                    Glide.with(binding.ivCover.getContext())
                            .load(coverUrl)
                            .placeholder(R.drawable.ic_book_placeholder)
                            .error(R.drawable.ic_book_placeholder)
                            .into(binding.ivCover);
                } else {
                    binding.ivCover.setImageResource(R.drawable.ic_book_placeholder);
                }
            }
            binding.tvReason.setText(item.getReason() != null ? item.getReason() : "");
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onClick(item);
            });
        }
    }
}
