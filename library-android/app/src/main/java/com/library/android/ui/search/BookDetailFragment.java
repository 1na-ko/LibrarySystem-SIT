package com.library.android.ui.search;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.library.android.R;
import com.library.android.databinding.FragmentBookDetailBinding;
import com.library.android.model.BookDetailVO;
import com.library.android.model.BookRecommendVO;
import com.library.android.model.BookSimpleVO;
import com.library.android.model.Result;
import com.library.android.network.exception.ApiException;
import com.library.android.repository.ReservationRepository;
import com.library.android.ui.borrow.BorrowConfirmDialog;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.viewmodel.BookDetailViewModel;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

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
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class BookDetailFragment extends Fragment {

    private static final String TAG = "BookDetailFragment";

    private FragmentBookDetailBinding binding;
    private BookDetailViewModel viewModel;
    private RecommendAdapter recommendAdapter;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    ReservationRepository reservationRepository;

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

        ((com.library.android.ui.main.MainActivity) requireActivity()).setGlobalTitle("图书详情");

        bookId = getArguments() != null ? getArguments().getLong("bookId", 0) : 0;

        // 相关推荐（WP1.5：popUpTo 自身+inclusive 防深栈溢出）
        recommendAdapter = new RecommendAdapter(item -> {
            if (item.getBook() == null) return;
            Bundle args = new Bundle();
            args.putLong("bookId", item.getBook().getId());
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
                        com.google.android.material.snackbar.Snackbar.make(requireView(),
                                getString(R.string.borrow_success_format, title),
                                com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
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

        viewModel.getErrorEvent().observe(getViewLifecycleOwner(), throwable -> {
            if (throwable != null && binding != null && !TextUtils.isEmpty(throwable.getMessage())) {
                Snackbar.make(binding.getRoot(), throwable.getMessage(), Snackbar.LENGTH_SHORT).show();
            }
        });
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
        v.setEnabled(false);  // 防抖：避免快速双击发起两次预约
        disposables.add(reservationRepository.reserveBook(currentDetail.getId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        result -> handleReserveResult(v, result),
                        throwable -> handleReserveError(v, throwable)
                ));
    }

    private void handleReserveResult(View v, Result<com.library.android.model.ReservationVO> result) {
        v.setEnabled(true);
        if (binding == null) return;
        if (result.isSuccess() && result.getData() != null) {
            int pos = result.getData().getQueuePosition();
            Snackbar.make(binding.getRoot(),
                    getString(R.string.reserve_success_format, pos),
                    Snackbar.LENGTH_LONG).show();
            // 刷新详情，更新预约人数
            viewModel.loadBookDetail(bookId);
        } else {
            Snackbar.make(binding.getRoot(),
                    !TextUtils.isEmpty(result.getMessage())
                            ? result.getMessage()
                            : getString(R.string.reserve_failed),
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    private void handleReserveError(View v, Throwable throwable) {
        v.setEnabled(true);
        if (binding == null) return;
        Log.e(TAG, "预约失败", throwable);
        String msg = (throwable instanceof ApiException
                && !TextUtils.isEmpty(((ApiException) throwable).getServerMessage()))
                ? ((ApiException) throwable).getServerMessage()
                : getString(R.string.reserve_failed);
        Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
    }

    private void onClickKnowledgeGraph(View v) {
        if (currentDetail == null) return;
        Bundle args = new Bundle();
        args.putLong("bookId", currentDetail.getId());
        Navigation.findNavController(v).navigate(R.id.action_bookDetailFragment_to_knowledgeGraphFragment, args);
    }

    /** 防 null/literal "null"/前后空格的安全文本兜底. */
    private static String safe(@Nullable String s) {
        return s == null ? "" : s.trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
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
            }
            binding.tvReason.setText(item.getReason() != null ? item.getReason() : "");
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onClick(item);
            });
        }
    }
}
