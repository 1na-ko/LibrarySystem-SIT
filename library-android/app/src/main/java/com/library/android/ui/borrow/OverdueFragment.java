package com.library.android.ui.borrow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.library.android.R;
import com.library.android.databinding.FragmentOverdueBinding;
import com.library.android.databinding.ItemBorrowRecordBinding;
import com.library.android.model.BorrowRecordVO;
import com.library.android.repository.BorrowRepository;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.LoadingState;
import com.library.android.ui.common.PagingScrollListener;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import javax.inject.Inject;

/**
 * 超期管理 Fragment — 管理员查看所有超期未还记录（人员 B 主导）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class OverdueFragment extends Fragment {

    private FragmentOverdueBinding binding;
    private OverdueAdapter adapter;
    private PagingScrollListener scrollListener;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private int currentPage = 1;
    private int totalPages = 0;
    private boolean isLoading = false;

    @Inject
    BorrowRepository borrowRepository;

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

        adapter = new OverdueAdapter();
        binding.toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);

        scrollListener = new PagingScrollListener(layoutManager) {
            @Override
            protected void loadMore() {
                loadMoreData();
            }
        };
        binding.recyclerView.addOnScrollListener(scrollListener);

        loadData();
    }

    private void loadData() {
        currentPage = 1;
        binding.progressBar.setVisibility(View.VISIBLE);

        disposables.add(borrowRepository.getOverdueRecords(currentPage, 20)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        List<BorrowRecordVO> records = result.getData().getRecords();
                        adapter.submitListSync(records);
                        totalPages = result.getData().getTotalPages();
                        binding.layoutEmpty.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
                        scrollListener.setHasMore(!records.isEmpty());
                    }
                }, throwable -> {
                    binding.progressBar.setVisibility(View.GONE);
                }));
    }

    private void loadMoreData() {
        if (isLoading || currentPage >= totalPages) return;
        isLoading = true;
        currentPage++;

        disposables.add(borrowRepository.getOverdueRecords(currentPage, 20)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    isLoading = false;
                    scrollListener.setLoading(false);
                    if (result != null && result.isSuccess() && result.getData() != null) {
                        List<BorrowRecordVO> current = new ArrayList<>(adapter.getCurrentList());
                        current.addAll(result.getData().getRecords());
                        adapter.submitListSync(current);
                        scrollListener.setHasMore(currentPage < result.getData().getTotalPages());
                    }
                }, throwable -> {
                    isLoading = false;
                    scrollListener.setLoading(false);
                }));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
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
            if (item.getFineAmount() != null && item.getFineAmount() > 0) {
                b.textFine.setVisibility(View.VISIBLE);
                b.textFine.setText(b.getRoot().getContext().getString(R.string.fine_overdue_format, item.getFineAmount()));
            }
        }

        static class DiffCallback extends androidx.recyclerview.widget.DiffUtil.ItemCallback<BorrowRecordVO> {
            @Override
            public boolean areItemsTheSame(@NonNull BorrowRecordVO o, @NonNull BorrowRecordVO n) {
                return o.getId() == n.getId();
            }
            @Override
            public boolean areContentsTheSame(@NonNull BorrowRecordVO o, @NonNull BorrowRecordVO n) {
                return o.getStatus().equals(n.getStatus());
            }
        }
    }
}