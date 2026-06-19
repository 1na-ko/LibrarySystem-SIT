package com.library.android.ui.borrow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.library.android.R;
import com.library.android.databinding.FragmentOverdueBinding;
import com.library.android.databinding.ItemBorrowRecordBinding;
import com.library.android.model.BorrowRecordVO;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.common.LoadingState;
import com.library.android.ui.common.PagingScrollListener;
import com.library.android.ui.main.MainActivity;
import com.library.android.viewmodel.OverdueViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 超期管理 Fragment — 管理员查看所有超期未还记录（WP2.2 重构为 MVVM）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class OverdueFragment extends BaseFragment {

    private FragmentOverdueBinding binding;
    private OverdueViewModel viewModel;
    private OverdueAdapter adapter;
    private PagingScrollListener scrollListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOverdueBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OverdueViewModel.class);

        ((MainActivity) requireActivity()).setGlobalTitle(getString(R.string.page_title_overdue));

        // WP-8：错误事件订阅（原版无 observeError，加载失败无提示）
        observeError(viewModel.getErrorEvent());

        adapter = new OverdueAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);

        scrollListener = new PagingScrollListener(layoutManager) {
            @Override
            protected void loadMore() {
                if (viewModel.hasMore()) {
                    viewModel.loadNextPage();
                }
            }
        };
        binding.recyclerView.addOnScrollListener(scrollListener);

        // 观察数据
        viewModel.getOverdueList().observe(getViewLifecycleOwner(), records -> {
            if (records != null) {
                adapter.submitListSync(records);
                binding.layoutEmpty.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
                scrollListener.setHasMore(viewModel.hasMore());
            }
        });

        // 观察加载状态
        viewModel.getLoadingState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            binding.textLoading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE);
            if (state == LoadingState.ERROR) {
                binding.layoutEmpty.setVisibility(View.GONE);
            }
        });

        viewModel.loadFirstPage();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** 超期记录 Adapter（复用 item_borrow_record 布局）. */
    private static class OverdueAdapter extends BaseAdapter<BorrowRecordVO, ItemBorrowRecordBinding> {

        OverdueAdapter() {
            super(R.layout.item_borrow_record, new DiffCallback());
        }

        @Override
        protected ItemBorrowRecordBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return ItemBorrowRecordBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(ItemBorrowRecordBinding b, BorrowRecordVO item, int position) {
            if (item.getBook() != null) {
                b.textBookTitle.setText(item.getBook().getTitle());
                b.textAuthor.setText(item.getBook().getAuthor());
            }
            b.textBorrowDate.setText(item.getBorrowDate());
            b.textDueDate.setText(item.getDueDate());
            b.textStatus.setText(b.getRoot().getContext().getString(R.string.status_overdue_short));
            if (item.getFineAmount() != null
                    && item.getFineAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                b.textFine.setVisibility(View.VISIBLE);
                b.textFine.setText(b.getRoot().getContext().getString(
                        R.string.fine_overdue_format, item.getFineAmountDouble()));
            }
        }

        static class DiffCallback extends androidx.recyclerview.widget.DiffUtil.ItemCallback<BorrowRecordVO> {
            @Override
            public boolean areItemsTheSame(@NonNull BorrowRecordVO o, @NonNull BorrowRecordVO n) {
                return o.getId() == n.getId();
            }
            @Override
            public boolean areContentsTheSame(@NonNull BorrowRecordVO o, @NonNull BorrowRecordVO n) {
                return java.util.Objects.equals(o.getStatus(), n.getStatus());
            }
        }
    }
}
