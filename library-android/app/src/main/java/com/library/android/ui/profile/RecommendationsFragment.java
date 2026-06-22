package com.library.android.ui.profile;

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
import com.library.android.R;
import com.library.android.databinding.FragmentRecommendationsBinding;
import com.library.android.databinding.ItemRecommendationBinding;
import com.library.android.model.BookRecommendVO;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.common.NavArgKeys;
import com.library.android.ui.main.MainActivity;
import com.library.android.viewmodel.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 个性化推荐 Fragment — 流式增强版.
 * <p>
 * 进入页面调 {@link HomeViewModel#loadRecommendationsStream()}：
 * <ol>
 *   <li>后端 SSE 首个 books 事件秒回 → 下方书目列表立即展示</li>
 *   <li>后续 reason 事件逐 token 流式 → 顶部 AI 卡片逐字渲染推荐导语（打字机效果）</li>
 * </ol>
 * 顶部 AI 卡片流式生成中显示小 loading，书目加载中（books 未到）显示全屏 loading.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class RecommendationsFragment extends BaseFragment {

    private FragmentRecommendationsBinding binding;
    private HomeViewModel viewModel;
    private RecommendationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecommendationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.recommendations));

        adapter = new RecommendationAdapter(item -> {
            long bookId = item.getBook() != null ? item.getBook().getId() : 0;
            if (bookId <= 0) return;
            Bundle args = new Bundle();
            args.putLong(NavArgKeys.BOOK_ID, bookId);
            androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.bookDetailFragment, args);
        });
        binding.rvRecommendations.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecommendations.setAdapter(adapter);

        binding.swipeRefresh.setOnRefreshListener(this::reload);

        setupObservers();
        // 首次进入：若已有书目数据则不重复请求，否则启动流式
        if (viewModel.getRecommendations().getValue() == null) {
            viewModel.loadRecommendationsStream();
        }
    }

    private void reload() {
        viewModel.loadRecommendationsStream();
    }

    private void setupObservers() {
        // 书目列表（books 事件秒回）
        viewModel.getRecommendations().observe(getViewLifecycleOwner(), books -> {
            binding.swipeRefresh.setRefreshing(false);
            binding.layoutLoading.setVisibility(View.GONE);
            if (books != null && !books.isEmpty()) {
                adapter.submitList(books);
                binding.layoutEmpty.setVisibility(View.GONE);
            } else if (books != null) {
                // books 已到但为空（无借阅历史）
                adapter.submitList(new ArrayList<>());
                binding.layoutEmpty.setVisibility(
                        Boolean.TRUE.equals(viewModel.isReasonStreaming().getValue())
                                ? View.GONE : View.VISIBLE);
            }
            // books == null：仍在加载，保持 loading（由 loading LiveData 控制）
        });

        // 书目加载中（books 未到）— WP4.1：HomeViewModel 使用 BaseViewModel 的 getLoadingState()
        viewModel.getLoadingState().observe(getViewLifecycleOwner(), state -> {
            boolean isLoading = state == com.library.android.ui.common.LoadingState.LOADING;
            boolean hasBooks = viewModel.getRecommendations().getValue() != null;
            binding.layoutLoading.setVisibility(
                    isLoading && !hasBooks ? View.VISIBLE : View.GONE);
            if (!isLoading) {
                binding.swipeRefresh.setRefreshing(false);
            }
        });

        // AI 导语逐 token 流式 → 顶部卡片逐字渲染
        viewModel.getAiReason().observe(getViewLifecycleOwner(), reason -> {
            if (!TextUtils.isEmpty(reason)) {
                binding.tvAiReason.setText(reason);
            }
        });

        // 导语流式 loading 指示
        viewModel.isReasonStreaming().observe(getViewLifecycleOwner(), streaming -> {
            binding.reasonLoading.setVisibility(
                    Boolean.TRUE.equals(streaming) ? View.VISIBLE : View.GONE);
            // 流式结束且书目空，显示空态
            if (!Boolean.TRUE.equals(streaming)) {
                List<BookRecommendVO> books = viewModel.getRecommendations().getValue();
                if (books == null || books.isEmpty()) {
                    binding.layoutEmpty.setVisibility(View.VISIBLE);
                    binding.layoutLoading.setVisibility(View.GONE);
                }
            }
        });

        // WP-7：错误提示统一改用 BaseFragment.observeError（原 Toast 改 Snackbar）
        observeError(viewModel.getErrorEvent());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ======================== RecommendationAdapter ========================

    private static class RecommendationAdapter extends BaseAdapter<BookRecommendVO, ItemRecommendationBinding> {

        interface OnBookClickListener { void onBookClick(BookRecommendVO item); }
        private final OnBookClickListener listener;

        RecommendationAdapter(OnBookClickListener listener) {
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
        protected ItemRecommendationBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return ItemRecommendationBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(ItemRecommendationBinding binding, BookRecommendVO item, int position) {
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
            // WP-14：推荐书本点击跳详情（callback 模式，与首页一致，最可靠）
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onBookClick(item);
            });
        }
    }
}
