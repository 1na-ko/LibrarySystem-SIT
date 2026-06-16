package com.library.android.ui.reservation;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.library.android.R;
import com.library.android.databinding.FragmentReservationListBinding;
import com.library.android.databinding.ItemReservationBinding;
import com.library.android.model.ReservationVO;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.PagingScrollListener;
import com.library.android.viewmodel.ReservationViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 我的预约列表 Fragment（人员 B 主导）.
 *
 * <p>状态筛选 Tab + 分页列表 + 左滑取消预约 + 下拉刷新排队位置。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class ReservationListFragment extends Fragment {

    private FragmentReservationListBinding binding;
    private ReservationViewModel viewModel;
    private ReservationAdapter adapter;
    private PagingScrollListener scrollListener;

    private static final String[] STATUS_TABS = {null, "WAITING", "NOTIFIED", "RESERVED", "COMPLETED", "CANCELLED"};
    private static final int[] TAB_TITLES = {R.string.tab_all, R.string.tab_waiting,
            R.string.tab_notified, R.string.tab_reserved, R.string.tab_completed, R.string.tab_cancelled};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReservationListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ReservationViewModel.class);

        setupTabs();
        setupRecyclerView();
        observeViewModel();
        loadData(0);
    }

    private void setupTabs() {
        for (int titleRes : TAB_TITLES) {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(titleRes));
        }
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadData(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new ReservationAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);

        // 左滑删除（取消预约）
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView,
                                  @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder,
                                  @NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getBindingAdapterPosition();
                ReservationVO item = adapter.getCurrentList().get(pos);
                if (item.canCancel()) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.confirm_cancel_reservation)
                            .setMessage(item.getBook() != null ? item.getBook().getTitle() : "")
                            .setPositiveButton(R.string.confirm, (d, w) -> viewModel.cancelReservation(item.getId()))
                            .setNegativeButton(R.string.cancel, (d, w) -> adapter.notifyItemChanged(pos))
                            .setOnCancelListener(d -> adapter.notifyItemChanged(pos))
                            .show();
                } else {
                    adapter.notifyItemChanged(pos);
                    Snackbar.make(binding.getRoot(), R.string.cannot_cancel, Snackbar.LENGTH_SHORT).show();
                }
            }
        }).attachToRecyclerView(binding.recyclerView);

        scrollListener = new PagingScrollListener(layoutManager) {
            @Override
            protected void loadMore() {
                viewModel.loadMore();
            }
        };
        binding.recyclerView.addOnScrollListener(scrollListener);

        binding.swipeRefresh.setOnRefreshListener(() -> {
            int pos = binding.tabLayout.getSelectedTabPosition();
            loadData(pos >= 0 ? pos : 0);
        });
    }

    private void observeViewModel() {
        viewModel.getLoadingState().observe(getViewLifecycleOwner(), state -> {
            binding.progressBar.setVisibility(
                    state == com.library.android.ui.common.LoadingState.LOADING
                            && adapter.getCurrentList().isEmpty() ? View.VISIBLE : View.GONE);
            binding.layoutEmpty.setVisibility(
                    state == com.library.android.ui.common.LoadingState.EMPTY ? View.VISIBLE : View.GONE);
            binding.swipeRefresh.setRefreshing(false);
        });

        viewModel.getReservationList().observe(getViewLifecycleOwner(), list -> {
            adapter.submitListSync(list);
            scrollListener.setLoading(false);
            scrollListener.setHasMore(list != null && !list.isEmpty());
        });

        viewModel.getCancelResult().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Snackbar.make(binding.getRoot(), R.string.cancel_success, Snackbar.LENGTH_SHORT).show();
                loadData(binding.tabLayout.getSelectedTabPosition());
            }
        });
    }

    private void loadData(int tabPosition) {
        String status = STATUS_TABS[Math.min(tabPosition, STATUS_TABS.length - 1)];
        viewModel.loadReservations(status);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** 预约记录 Adapter. */
    private class ReservationAdapter extends BaseAdapter<ReservationVO, ItemReservationBinding> {

        ReservationAdapter() {
            super(R.layout.item_reservation, new DiffCallback());
        }

        @Override
        protected ItemReservationBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return ItemReservationBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(ItemReservationBinding b, ReservationVO item, int position) {
            if (item.getBook() != null) {
                b.textBookTitle.setText(item.getBook().getTitle());
                b.textAuthor.setText(item.getBook().getAuthor());
            }
            b.textReserveTime.setText(item.getReserveTime() != null
                    ? item.getReserveTime().substring(0, Math.min(10, item.getReserveTime().length())) : "");
            b.textStatus.setText(getStatusText(item.getStatus()));

            if (item.getQueuePosition() > 0) {
                b.textQueuePosition.setText(getString(R.string.queue_position_format, item.getQueuePosition()));
                b.textQueuePosition.setVisibility(View.VISIBLE);
            } else {
                b.textQueuePosition.setVisibility(View.GONE);
            }
        }

        private String getStatusText(String status) {
            if (status == null) return "";
            switch (status) {
                case "WAITING": return getString(R.string.status_waiting);
                case "NOTIFIED": return getString(R.string.status_notified);
                case "RESERVED": return getString(R.string.status_reserved);
                case "COMPLETED": return getString(R.string.status_completed);
                case "CANCELLED": return getString(R.string.status_cancelled);
                case "EXPIRED": return getString(R.string.status_expired);
                default: return status;
            }
        }

        class DiffCallback extends androidx.recyclerview.widget.DiffUtil.ItemCallback<ReservationVO> {
            @Override
            public boolean areItemsTheSame(@NonNull ReservationVO o, @NonNull ReservationVO n) {
                return o.getId() == n.getId();
            }
            @Override
            public boolean areContentsTheSame(@NonNull ReservationVO o, @NonNull ReservationVO n) {
                return o.getStatus().equals(n.getStatus())
                        && o.getQueuePosition() == n.getQueuePosition();
            }
        }
    }
}
