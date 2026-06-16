package com.library.android.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.library.android.R;
import com.library.android.databinding.FragmentAdminUserListBinding;
import com.library.android.databinding.ItemUserManageBinding;
import com.library.android.model.UserManageVO;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.PagingScrollListener;
import com.library.android.viewmodel.AdminViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 用户管理 Fragment — 管理员查看/筛选/状态变更用户（人员 B 主导）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class AdminUserListFragment extends Fragment {

    private FragmentAdminUserListBinding binding;
    private AdminViewModel viewModel;
    private UserAdapter adapter;
    private PagingScrollListener scrollListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminUserListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        setupRecyclerView();
        setupSearch();
        observeViewModel();
        viewModel.loadUsers(null, null, null);
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);

        scrollListener = new PagingScrollListener(layoutManager) {
            @Override
            protected void loadMore() {
                viewModel.loadMoreUsers();
            }
        };
        binding.recyclerView.addOnScrollListener(scrollListener);
    }

    private void setupSearch() {
        binding.editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String keyword = binding.editSearch.getText() != null
                        ? binding.editSearch.getText().toString().trim() : null;
                viewModel.loadUsers(null, null, keyword);
                return true;
            }
            return false;
        });

        binding.btnFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        String[] roleLabels = {getString(R.string.all_filter), getString(R.string.role_student),
                getString(R.string.role_teacher), getString(R.string.role_librarian),
                getString(R.string.role_acquisitor), getString(R.string.role_admin)};
        String[] roleValues = {null, "STUDENT", "TEACHER", "LIBRARIAN", "ACQUISITOR", "ADMIN"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.filter_users)
                .setItems(roleLabels, (dialog, which) -> {
                    viewModel.loadUsers(roleValues[which], null, null);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void observeViewModel() {
        viewModel.getUserLoadingState().observe(getViewLifecycleOwner(), state -> {
            binding.progressBar.setVisibility(
                    state == com.library.android.ui.common.LoadingState.LOADING
                            && adapter.getCurrentList().isEmpty() ? View.VISIBLE : View.GONE);
            binding.layoutEmpty.setVisibility(
                    state == com.library.android.ui.common.LoadingState.EMPTY ? View.VISIBLE : View.GONE);
        });

        viewModel.getUserList().observe(getViewLifecycleOwner(), list -> {
            adapter.submitListSync(list);
            scrollListener.setLoading(false);
            scrollListener.setHasMore(list != null && !list.isEmpty());
        });

        viewModel.getStatusUpdateResult().observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success)) {
                Snackbar.make(binding.getRoot(), R.string.status_updated, Snackbar.LENGTH_SHORT).show();
                viewModel.loadUsers(null, null, null);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** 用户管理 Adapter. */
    private class UserAdapter extends BaseAdapter<UserManageVO, ItemUserManageBinding> {

        UserAdapter() {
            super(R.layout.item_user_manage, new DiffCallback());
        }

        @Override
        protected ItemUserManageBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return ItemUserManageBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(ItemUserManageBinding b, UserManageVO item, int position) {
            b.textRealName.setText(item.getRealName());
            b.textUsername.setText(item.getUsername());
            b.textRole.setText(getRoleLabel(item.getRole()));
            b.textStatus.setText(getStatusLabel(item.getStatus()));
            b.textBorrows.setText(getString(R.string.borrow_count_format, item.getCurrentBorrows()));
            b.textOverdue.setText(getString(R.string.overdue_count_format, item.getTotalOverdue()));

            b.itemView.setOnClickListener(v -> showUserActionDialog(item));
        }

        private void showUserActionDialog(UserManageVO user) {
            String[] actions;
            if (user.isActive()) {
                actions = new String[]{getString(R.string.freeze_user), getString(R.string.disable_user)};
            } else if (user.isFrozen()) {
                actions = new String[]{getString(R.string.unfreeze_user), getString(R.string.disable_user)};
            } else {
                actions = new String[]{getString(R.string.unfreeze_user)};
            }

            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.user_action_title_format,
                            user.getRealName(), user.getUsername()))
                    .setItems(actions, (dialog, which) -> {
                        String newStatus;
                        if (user.isActive()) {
                            newStatus = which == 0 ? "FROZEN" : "DISABLED";
                        } else if (user.isFrozen()) {
                            newStatus = which == 0 ? "ACTIVE" : "DISABLED";
                        } else {
                            newStatus = "ACTIVE";
                        }
                        viewModel.updateUserStatus(user.getId(), newStatus);
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }

        private String getRoleLabel(String role) {
            if (role == null) return "";
            switch (role) {
                case "STUDENT": return getString(R.string.role_student);
                case "TEACHER": return getString(R.string.role_teacher);
                case "LIBRARIAN": return getString(R.string.role_librarian);
                case "ACQUISITOR": return getString(R.string.role_acquisitor);
                case "ADMIN": return getString(R.string.role_admin);
                default: return role;
            }
        }

        private String getStatusLabel(String status) {
            if (status == null) return "";
            switch (status) {
                case "ACTIVE": return getString(R.string.status_active);
                case "FROZEN": return getString(R.string.status_frozen);
                case "DISABLED": return getString(R.string.status_disabled);
                default: return status;
            }
        }

        class DiffCallback extends androidx.recyclerview.widget.DiffUtil.ItemCallback<UserManageVO> {
            @Override
            public boolean areItemsTheSame(@NonNull UserManageVO o, @NonNull UserManageVO n) {
                return o.getId() == n.getId();
            }
            @Override
            public boolean areContentsTheSame(@NonNull UserManageVO o, @NonNull UserManageVO n) {
                return o.getStatus().equals(n.getStatus());
            }
        }
    }
}
