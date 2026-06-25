package com.library.android.ui.common;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * 分页滚动监听器 — 当 RecyclerView 滚动到底部时触发加载更多.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public abstract class PagingScrollListener extends RecyclerView.OnScrollListener {

    private final LinearLayoutManager layoutManager;
    private boolean isLoading = false;
    private boolean hasMore = true;

    public PagingScrollListener(LinearLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

        if (!isLoading && hasMore && dy > 0) {
            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                    && firstVisibleItemPosition >= 0) {
                isLoading = true;
                loadMore();
            }
        }
    }

    /** 加载更多数据的回调. */
    protected abstract void loadMore();

    public void setLoading(boolean loading) {
        this.isLoading = loading;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public boolean isLoading() {
        return isLoading;
    }
}
