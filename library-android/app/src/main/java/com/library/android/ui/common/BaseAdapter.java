package com.library.android.ui.common;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import java.util.List;

/**
 * 通用 RecyclerView Adapter 基类 — 配合 ViewBinding 简化列表实现.
 *
 * <p>基于 {@link ListAdapter} + {@link DiffUtil} 自动处理增量更新。
 *
 * @param <T>  列表项数据类型（需正确覆写 equals）
 * @param <VB> ViewBinding 类型
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public abstract class BaseAdapter<T, VB extends ViewBinding> extends ListAdapter<T, BaseAdapter<T, VB>.ViewHolder> {

    private final int layoutResId;

    protected BaseAdapter(@LayoutRes int layoutResId, @NonNull DiffUtil.ItemCallback<T> diffCallback) {
        super(diffCallback);
        this.layoutResId = layoutResId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        VB binding = createBinding(LayoutInflater.from(parent.getContext()), parent);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        bind(holder.binding, getItem(position), position);
    }

    /** 子类覆写此方法创建 ViewBinding. */
    protected abstract VB createBinding(LayoutInflater inflater, ViewGroup parent);

    /** 子类覆写此方法绑定数据到视图. */
    protected abstract void bind(VB binding, T item, int position);

    /** 便捷更新整个列表（ListAdapter 内部使用 DiffUtil 计算差异）. */
    public void submitListSync(List<T> list) {
        submitList(list != null ? list : java.util.Collections.emptyList());
    }

    /** ViewHolder，持有 ViewBinding 引用. */
    public class ViewHolder extends RecyclerView.ViewHolder {
        public final VB binding;

        public ViewHolder(@NonNull VB binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
