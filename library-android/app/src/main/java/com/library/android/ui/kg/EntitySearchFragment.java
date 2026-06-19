package com.library.android.ui.kg;

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

import com.library.android.R;
import com.library.android.databinding.FragmentEntitySearchBinding;
import com.library.android.databinding.ItemEntitySearchBinding;
import com.library.android.model.EntitySearchResult;
import com.library.android.ui.common.BaseAdapter;
import com.library.android.ui.common.BaseFragment;
import com.library.android.ui.common.LoadingState;
import com.library.android.ui.main.MainActivity;
import com.library.android.ui.theme.ThemeManager;
import com.library.android.viewmodel.KnowledgeGraphViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * 知识实体搜索 Fragment（人员 B 主导）.
 *
 * <p>按实体类型筛选（图书/作者/关键词/学科），展示 PageRank 中心度。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@AndroidEntryPoint
public class EntitySearchFragment extends BaseFragment {

    private FragmentEntitySearchBinding binding;
    private KnowledgeGraphViewModel viewModel;
    private EntityAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context themedContext = ThemeManager.getInstance().wrapContext(requireContext());
        android.view.LayoutInflater themedInflater = inflater.cloneInContext(themedContext);
        binding = FragmentEntitySearchBinding.inflate(themedInflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(KnowledgeGraphViewModel.class);

        ((MainActivity) requireActivity()).setGlobalTitle("实体搜索");

        setupRecyclerView();
        setupSearch();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new EntityAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        binding.btnSearch.setOnClickListener(v -> performSearch());
    }

    private void performSearch() {
        String query = binding.editSearch.getText() != null
                ? binding.editSearch.getText().toString().trim() : "";
        if (query.isEmpty()) return;

        // 确定选中的 entity type
        String type = null;
        int checkedId = binding.chipGroup.getCheckedChipId();
        if (checkedId == com.library.android.R.id.chipBook) type = "BOOK";
        else if (checkedId == com.library.android.R.id.chipAuthor) type = "AUTHOR";
        else if (checkedId == com.library.android.R.id.chipKeyword) type = "KEYWORD";
        else if (checkedId == com.library.android.R.id.chipSubject) type = "SUBJECT";
        // chipAll → type=null 表示搜索全部类型

        viewModel.searchEntities(query, type);
    }

    private void observeViewModel() {
        viewModel.getEntityResults().observe(getViewLifecycleOwner(), results -> {
            adapter.submitListSync(results);
            // WP-6：空结果友好提示（原版仅列表空白，用户无感知）
            boolean empty = results == null || results.isEmpty();
            if (binding != null) {
                binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
        });

        // WP-6 P0：Loading 类型修复（原 Boolean.TRUE.equals(LoadingState) 永远 false）
        viewModel.getLoadingState().observe(getViewLifecycleOwner(), state ->
                binding.textLoading.setVisibility(state == LoadingState.LOADING ? View.VISIBLE : View.GONE));

        // WP-6：错误事件订阅
        observeError(viewModel.getErrorEvent());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /** 实体搜索结果 Adapter. */
    private static class EntityAdapter extends BaseAdapter<EntitySearchResult, ItemEntitySearchBinding> {

        EntityAdapter() {
            super(R.layout.item_entity_search, new DiffCallback());
        }

        @Override
        protected ItemEntitySearchBinding createBinding(LayoutInflater inflater, ViewGroup parent) {
            return ItemEntitySearchBinding.inflate(inflater, parent, false);
        }

        @Override
        protected void bind(ItemEntitySearchBinding b, EntitySearchResult item, int position) {
            b.textName.setText(item.getEntityName());
            b.textType.setText(getTypeLabel(b.getRoot().getContext(), item.getEntityType()));
            b.textPagerank.setText(String.format("PR: %.4f", item.getPagerank()));
        }

        private String getTypeLabel(android.content.Context ctx, String type) {
            if (type == null) return "";
            switch (type) {
                case "BOOK": return ctx.getString(R.string.entity_type_book);
                case "AUTHOR": return ctx.getString(R.string.entity_type_author);
                case "KEYWORD": return ctx.getString(R.string.entity_type_keyword);
                case "SUBJECT": return ctx.getString(R.string.entity_type_subject);
                default: return type;
            }
        }

        static class DiffCallback extends androidx.recyclerview.widget.DiffUtil.ItemCallback<EntitySearchResult> {
            @Override
            public boolean areItemsTheSame(@NonNull EntitySearchResult o, @NonNull EntitySearchResult n) {
                return o.getEntityId() == n.getEntityId();
            }
            @Override
            public boolean areContentsTheSame(@NonNull EntitySearchResult o, @NonNull EntitySearchResult n) {
                return java.util.Objects.equals(o.getEntityName(), n.getEntityName());
            }
        }
    }
}