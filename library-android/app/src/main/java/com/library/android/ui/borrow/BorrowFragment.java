package com.library.android.ui.borrow;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.library.android.R;
import com.library.android.databinding.FragmentBorrowBinding;
import com.library.android.databinding.ItemBorrowRecordBinding;
import com.library.android.model.BorrowRecordVO;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.PagingScrollListener;
import com.library.android.viewmodel.BorrowViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 借阅管理首页 Fragment（人员 B 主导）.
 *
 * <p>Tab 切换：全部 / 借阅中 / 已归还 / 已超期；
 * 支持分页加载、下拉刷新、条码扫描入口。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class BorrowFragment extends Fragment {

    private FragmentBorrowBinding binding;
    private BorrowViewModel viewModel;
    private BorrowAdapter adapter;
    private PagingScrollListener scrollListener;

    private static final String[] STATUS_TABS = {null, "BORROWED", "RETURNED", "OVERDUE"};
    private static final int[] TAB_TITLES = {R.string.tab_all, R.string.tab_borrowing,
            R.string.tab_returned, R.string.tab_overdue};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentBorrowBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(BorrowViewModel.class);

        setupTabs();
        setupRecyclerView();
        setupFab();
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
        adapter = new BorrowAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);

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

    private void setupFab() {
        binding.fabScanBorrow.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireContext(),
                        Class.forName("com.library.android.ui.scanner.ScanBarcodeActivity"));
                intent.putExtra("source", "borrow");
                startActivity(intent);
            } catch (ClassNotFoundException e) {
                Snackbar.make(binding.getRoot(), R.string.scanner_unavailable, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getLoadingState().observe(getViewLifecycleOwner(), state -> {
            binding.progressBar.setVisibility(
                    state == com.library.android.ui.common.LoadingState.LOADING
                            && adapter.getCurrentList().isEmpty() ? View.VISIBLE : View.GONE);
            binding.layoutEmpty.setVisibility(
                    state == com.library.android.ui.common.LoadingState.EMPTY ? View.VISIBLE : View.GONE);
            binding.layoutError.setVisibility(
                    state == com.library.android.ui.common.LoadingState.ERROR ? View.VISIBLE : View.GONE);
            binding.swipeRefresh.setRefreshing(false);
        });

        viewModel.getBorrowList().observe(getViewLifecycleOwner(), list -> {
            adapter.submitListSync(list);
            scrollListener.setLoading(false);
            scrollListener.setHasMore(list != null && !list.isEmpty());
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                binding.textError.setText(msg);
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void loadData(int tabPosition) {
        String status = STATUS_TABS[Math.min(tabPosition, STATUS_TABS.length - 1)];
        viewModel.loadBorrows(status);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** 借阅记录 Adapter. */
    private class BorrowAdapter extends BaseAdapter<BorrowRecordVO, ItemBorrowRecordBinding> {

        BorrowAdapter() {
            super(R.layout.item_borrow_record, new DiffCallback());
        }

        @Override
        protected ItemBorrowRecordBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return ItemBorrowRecordBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(ItemBorrowRecordBinding b, BorrowRecordVO item, int position) {
            b.textBookTitle.setText(item.getBook() != null ? item.getBook().getTitle() : "");
            b.textAuthor.setText(item.getBook() != null ? item.getBook().getAuthor() : "");
            b.textBorrowDate.setText(getString(R.string.borrow_date_format, item.getBorrowDate()));
            b.textDueDate.setText(getString(R.string.due_date_format, item.getDueDate()));
            b.textStatus.setText(getStatusText(item.getStatus()));

            if (item.getFineAmount() != null && item.getFineAmount() > 0) {
                b.textFine.setVisibility(View.VISIBLE);
                b.textFine.setText(getString(R.string.fine_format, item.getFineAmount()));
            } else {
                b.textFine.setVisibility(View.GONE);
            }

            b.itemView.setOnClickListener(v -> {
                Bundle args = new Bundle();
                args.putLong("borrowId", item.getId());
                Navigation.findNavController(v).navigate(
                        R.id.action_borrowFragment_to_borrowDetailFragment, args);
            });
        }

        private String getStatusText(String status) {
            if (status == null) return "";
            switch (status) {
                case "BORROWED": return getString(R.string.status_borrowed);
                case "RENEWED": return getString(R.string.status_renewed);
                case "RETURNED": return getString(R.string.status_returned);
                case "OVERDUE": return getString(R.string.status_overdue);
                default: return status;
            }
        }

        class DiffCallback extends androidx.recyclerview.widget.DiffUtil.ItemCallback<BorrowRecordVO> {
            @Override
            public boolean areItemsTheSame(@NonNull BorrowRecordVO old, @NonNull BorrowRecordVO n) {
                return old.getId() == n.getId();
            }
            @Override
            public boolean areContentsTheSame(@NonNull BorrowRecordVO old, @NonNull BorrowRecordVO n) {
                return old.getStatus().equals(n.getStatus())
                        && old.getDueDate().equals(n.getDueDate());
            }
        }
    }
}
