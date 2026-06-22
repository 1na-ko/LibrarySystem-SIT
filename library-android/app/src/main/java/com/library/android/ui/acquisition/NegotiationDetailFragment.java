package com.library.android.ui.acquisition;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.library.android.R;
import com.library.android.databinding.FragmentNegotiationDetailBinding;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.common.NavArgKeys;
import com.library.android.ui.main.MainActivity;
import com.library.android.viewmodel.AcquisitionViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 谈判建议详情 Fragment — WP6 流式版.
 *
 * <p>价格区间秒回（priceRange 事件）+ AI 谈判文本逐 token 流式（text 事件，打字机效果）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class NegotiationDetailFragment extends BaseFragment {

    private FragmentNegotiationDetailBinding binding;
    private AcquisitionViewModel viewModel;
    private long negotiationId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        com.library.android.ui.theme.ThemeManager.getInstance().setDarkMode(true);
        android.content.Context themedContext = com.library.android.ui.theme.ThemeManager.getInstance().wrapContext(requireContext());
        android.view.LayoutInflater themedInflater = inflater.cloneInContext(themedContext);
        binding = FragmentNegotiationDetailBinding.inflate(themedInflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // WP-11：改 Fragment scope VM，避免 Activity scope 共享导致返回再进入重复触发 AI 流式
        viewModel = new ViewModelProvider(this).get(AcquisitionViewModel.class);
        ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.page_title_negotiation_detail));

        if (getArguments() != null) {
            negotiationId = getArguments().getLong(NavArgKeys.NEGOTIATION_ID, 0);
        }

        observeError(viewModel.getErrorEvent());

        // 价格区间（秒回）
        viewModel.getPriceRange().observe(getViewLifecycleOwner(), pr -> {
            if (pr == null || binding == null) return;
            binding.tvPriceRange.setText(getString(R.string.negotiation_price_format,
                    pr.getFloorPrice() != null ? pr.getFloorPrice().toPlainString() : "—",
                    pr.getMedianPrice() != null ? pr.getMedianPrice().toPlainString() : "—",
                    pr.getCeilingPrice() != null ? pr.getCeilingPrice().toPlainString() : "—",
                    pr.getSuggestedOffer() != null ? pr.getSuggestedOffer().toPlainString() : "—"));
        });

        // AI 文本（逐 token）
        viewModel.getSuggestionText().observe(getViewLifecycleOwner(), text -> {
            if (binding == null) return;
            if (!TextUtils.isEmpty(text)) {
                binding.tvSuggestionText.setText(text);
            }
        });

        // 流式 loading
        viewModel.isSuggestionStreaming().observe(getViewLifecycleOwner(), streaming -> {
            if (binding == null) return;
            binding.streamLoading.setVisibility(Boolean.TRUE.equals(streaming) ? View.VISIBLE : View.GONE);
        });

        // WP-11：仅当尚未加载过该 negotiation 的建议时才启动流式，避免返回再进入重复触发
        if (negotiationId > 0 && !viewModel.isSuggestionLoadedFor(negotiationId)) {
            viewModel.streamNegotiationSuggestion(negotiationId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 主动中断 SSE 流，防止 IO 线程回调在 Fragment 销毁后触发（修复谈判流式闪退）
        if (viewModel != null) {
            viewModel.disposeStreams();
        }
        binding = null;
    }
}
