package com.library.android.ui.main;

import android.os.Bundle;
import android.text.TextUtils;
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

import com.library.android.R;
import com.library.android.databinding.FragmentHomeBinding;
import com.library.android.databinding.ItemBookBinding;
import com.library.android.model.BookRecommendVO;
import com.library.android.model.BookSimpleVO;
import com.library.android.model.CategoryVO;
import com.library.android.repository.BookRepository;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.viewmodel.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 混合首页 Fragment（WP1）— 推荐卡片（AI 导语流式）+ 热门图书 + 分类导航.
 * <p>
 * 推荐复用 {@link HomeViewModel#loadRecommendationsStream()}（activity scope，与 RecommendationsFragment 共享），
 * 热门/分类通过 {@link BookRepository} 加载. 顶部不放搜索框，搜索由 MainActivity 全局搜索按钮进入 SearchFragment 二级页.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel homeViewModel;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    BookRepository bookRepository;

    private HomeRecommendAdapter recommendAdapter;
    private HotBookAdapter hotBookAdapter;
    private CategoryAdapter categoryAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        homeViewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.home_title));

        // 推荐前 3 本
        recommendAdapter = new HomeRecommendAdapter(book -> navigateToBookDetail(book.getBook() != null ? book.getBook().getId() : 0));
        binding.rvHomeRecommendations.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvHomeRecommendations.setAdapter(recommendAdapter);

        // 热门图书
        hotBookAdapter = new HotBookAdapter(book -> navigateToBookDetail(book.getId()));
        binding.rvHotBooks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHotBooks.setAdapter(hotBookAdapter);

        // 分类导航：WP-3 改横向滚动卡片，点击分类 → 跳搜索页按分类搜
        categoryAdapter = new CategoryAdapter(this::navigateToSearchByCategory);
        binding.rvCategories.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategories.setAdapter(categoryAdapter);

        // 查看全部推荐 → RecommendationsFragment
        binding.tvAllRecommendations.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_recommendationsFragment));

        // WP1.2：查看全部热门图书 → HotBooksFragment
        binding.tvAllHotBooks.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_hotBooksFragment));

        // WP-3：浏览全部分类 → CategoryTreeFragment
        binding.tvAllCategories.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_homeFragment_to_categoryTreeFragment));

        setupRecommendObservers();
        loadHotBooks();
        loadCategories();

        // 首次进入触发推荐流式（若未加载）
        if (homeViewModel.getRecommendations().getValue() == null) {
            homeViewModel.loadRecommendationsStream();
        }
    }

    /** 推荐区观察：AI 导语流式 + 前 3 本推荐 + 流式 loading. */
    private void setupRecommendObservers() {
        homeViewModel.getAiReason().observe(getViewLifecycleOwner(), reason -> {
            if (!TextUtils.isEmpty(reason)) {
                binding.tvAiReason.setText(reason);
            }
        });
        homeViewModel.isReasonStreaming().observe(getViewLifecycleOwner(), streaming ->
                binding.reasonLoading.setVisibility(Boolean.TRUE.equals(streaming) ? View.VISIBLE : View.GONE));
        homeViewModel.getRecommendations().observe(getViewLifecycleOwner(), books -> {
            if (books != null && !books.isEmpty()) {
                // 首页只显前 3 本
                List<BookRecommendVO> top3 = books.size() > 3 ? new ArrayList<>(books.subList(0, 3)) : books;
                recommendAdapter.submitList(top3);
                binding.tvAllRecommendations.setVisibility(View.VISIBLE);
            } else {
                recommendAdapter.submitList(new ArrayList<>());
                binding.tvAllRecommendations.setVisibility(View.GONE);
            }
        });
    }

    private void loadHotBooks() {
        disposables.add(bookRepository.getHotBooks(null, 5)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        hotBookAdapter.submitList(result.getData());
                    }
                }, throwable -> {
                    // WP-3：加载失败提示（原 printStackTrace 用户无感知）
                    View root = getView();
                    if (root != null) {
                        com.google.android.material.snackbar.Snackbar.make(root,
                                R.string.home_load_failed, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                    }
                }));
    }

    private void loadCategories() {
        disposables.add(bookRepository.getCategoryTree()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        categoryAdapter.submitList(result.getData());
                    }
                }, throwable -> {
                    View root = getView();
                    if (root != null) {
                        com.google.android.material.snackbar.Snackbar.make(root,
                                R.string.home_load_failed, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                    }
                }));
    }

    private void navigateToBookDetail(long bookId) {
        if (bookId <= 0) return;
        Bundle args = new Bundle();
        args.putLong("bookId", bookId);
        Navigation.findNavController(requireView()).navigate(R.id.action_homeFragment_to_bookDetailFragment, args);
    }

    private void navigateToSearchByCategory(CategoryVO category) {
        android.content.Intent intent = new android.content.Intent(requireContext(),
                com.library.android.ui.search.SearchActivity.class);
        intent.putExtra("categoryId", category.getId());
        intent.putExtra("categoryName", category.getName() != null ? category.getName() : "");
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
        binding = null;
    }

    // ======================== Adapters ========================

    private static class HomeRecommendAdapter extends BaseAdapter<BookRecommendVO, ItemBookBinding> {
        interface OnClick { void onClick(BookRecommendVO book); }
        private final OnClick listener;

        HomeRecommendAdapter(OnClick listener) {
            super(R.layout.item_book, new DiffUtil.ItemCallback<BookRecommendVO>() {
                @Override
                public boolean areItemsTheSame(@NonNull BookRecommendVO o, @NonNull BookRecommendVO n) {
                    return o.getBook() != null && n.getBook() != null && o.getBook().getId() == n.getBook().getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull BookRecommendVO o, @NonNull BookRecommendVO n) {
                    return true;
                }
            });
            this.listener = listener;
        }
        @Override
        protected ItemBookBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return ItemBookBinding.inflate(inflater, parent, false);
        }
        @Override
        protected void bind(ItemBookBinding b, BookRecommendVO item, int position) {
            if (item.getBook() != null) {
                b.tvTitle.setText(item.getBook().getTitle() != null ? item.getBook().getTitle() : "");
                b.tvAuthor.setText(item.getBook().getAuthor() != null ? item.getBook().getAuthor() : "");
                b.tvAvailCopies.setText(b.getRoot().getContext().getString(R.string.avail_copies_format, item.getBook().getAvailCopies()));
            }
            b.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }

    private static class HotBookAdapter extends BaseAdapter<BookSimpleVO, ItemBookBinding> {
        interface OnClick { void onClick(BookSimpleVO book); }
        private final OnClick listener;

        HotBookAdapter(OnClick listener) {
            super(R.layout.item_book, new DiffUtil.ItemCallback<BookSimpleVO>() {
                @Override
                public boolean areItemsTheSame(@NonNull BookSimpleVO o, @NonNull BookSimpleVO n) {
                    return o.getId() == n.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull BookSimpleVO o, @NonNull BookSimpleVO n) {
                    return o.getAvailCopies() == n.getAvailCopies();
                }
            });
            this.listener = listener;
        }
        @Override
        protected ItemBookBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return ItemBookBinding.inflate(inflater, parent, false);
        }
        @Override
        protected void bind(ItemBookBinding b, BookSimpleVO item, int position) {
            b.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "");
            b.tvAuthor.setText(item.getAuthor() != null ? item.getAuthor() : "");
            // WP-3：资源化"可借 N"（原硬编码 "可借 "）
            b.tvAvailCopies.setText(b.getRoot().getContext().getString(R.string.avail_copies_format, item.getAvailCopies()));
            b.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }

    private static class CategoryAdapter extends BaseAdapter<CategoryVO, com.library.android.databinding.ItemCategoryHomeBinding> {
        interface OnClick { void onClick(CategoryVO category); }
        private final OnClick listener;

        CategoryAdapter(OnClick listener) {
            super(R.layout.item_category_home, new DiffUtil.ItemCallback<CategoryVO>() {
                @Override
                public boolean areItemsTheSame(@NonNull CategoryVO o, @NonNull CategoryVO n) {
                    return o.getId() == n.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull CategoryVO o, @NonNull CategoryVO n) {
                    return o.getName() != null ? o.getName().equals(n.getName()) : n.getName() == null;
                }
            });
            this.listener = listener;
        }
        @Override
        protected com.library.android.databinding.ItemCategoryHomeBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return com.library.android.databinding.ItemCategoryHomeBinding.inflate(inflater, parent, false);
        }
        @Override
        protected void bind(com.library.android.databinding.ItemCategoryHomeBinding b, CategoryVO item, int position) {
            b.tvCategoryName.setText(item.getName() != null ? item.getName() : "");
            // WP-3：主页横向卡片不显子分类数量（字段 tvBookCount 存在但后端 CategoryVO 无此字段，留空）
            b.tvBookCount.setText("");
            b.getRoot().setOnClickListener(v -> listener.onClick(item));
        }
    }
}
