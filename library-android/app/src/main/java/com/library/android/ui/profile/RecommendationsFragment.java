package com.library.android.ui.profile;

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

import com.library.android.R;
import com.library.android.databinding.FragmentRecommendationsBinding;
import com.library.android.databinding.ItemRecommendationBinding;
import com.library.android.model.BookRecommendVO;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.viewmodel.ProfileViewModel;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 个性化推荐 Fragment.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class RecommendationsFragment extends Fragment {

    private FragmentRecommendationsBinding binding;
    private ProfileViewModel viewModel;
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
        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        adapter = new RecommendationAdapter();
        binding.rvRecommendations.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvRecommendations.setAdapter(adapter);

        // 下拉刷新
        binding.swipeRefresh.setOnRefreshListener(() -> viewModel.loadRecommendations());

        setupObservers();
        viewModel.loadRecommendations();
    }

    private void setupObservers() {
        viewModel.getRecommendations().observe(getViewLifecycleOwner(), recommendations -> {
            binding.swipeRefresh.setRefreshing(false);
            if (recommendations != null && !recommendations.isEmpty()) {
                adapter.submitList(recommendations);
                binding.layoutEmpty.setVisibility(View.GONE);
            } else {
                adapter.submitList(new ArrayList<>());
                binding.layoutEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ======================== RecommendationAdapter ========================

    private static class RecommendationAdapter extends BaseAdapter<BookRecommendVO, ItemRecommendationBinding> {

        RecommendationAdapter() {
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
        protected ItemRecommendationBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return ItemRecommendationBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(ItemRecommendationBinding binding, BookRecommendVO item, int position) {
            binding.tvTitle.setText(item.getBook().getTitle());
            binding.tvAuthor.setText(item.getBook().getAuthor());
            binding.tvReason.setText("推荐理由: " + item.getReason());
            binding.tvScore.setText(String.format("%.0f", item.getScore()));
        }
    }
}