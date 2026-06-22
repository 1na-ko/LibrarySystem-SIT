package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 流通统计 Dashboard VO — 与后端 DashboardVO 字段对齐（C.2 新增）.
 *
 * <p>用于管理端 Dashboard 渲染：
 * <ul>
 *   <li>4 张数字卡片（今日借/还/超期 + 实时活跃读者）</li>
 *   <li>本月日趋势（折线图：borrows / returns 双线）</li>
 *   <li>热门分类 Top-10（饼图）</li>
 * </ul>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class DashboardVO {

    @SerializedName("todayBorrows")
    private long todayBorrows;

    @SerializedName("todayReturns")
    private long todayReturns;

    @SerializedName("todayOverdue")
    private long todayOverdue;

    @SerializedName("activeBorrowers")
    private long activeBorrowers;

    @SerializedName("monthTrend")
    private List<DailyTrend> monthTrend;

    @SerializedName("hotCategories")
    private List<CategoryHotStat> hotCategories;

    public long getTodayBorrows() { return todayBorrows; }
    public long getTodayReturns() { return todayReturns; }
    public long getTodayOverdue() { return todayOverdue; }
    public long getActiveBorrowers() { return activeBorrowers; }
    public List<DailyTrend> getMonthTrend() { return monthTrend; }
    public List<CategoryHotStat> getHotCategories() { return hotCategories; }

    /** 单日借/还趋势点. */
    public static class DailyTrend {
        @SerializedName("date")
        private String date;
        @SerializedName("borrows")
        private long borrows;
        @SerializedName("returns")
        private long returns;

        public String getDate() { return date; }
        public long getBorrows() { return borrows; }
        public long getReturns() { return returns; }
    }

    /** 热门分类统计项 — WP-4 契约对齐：与后端 DashboardVO.CategoryHotStat 一致（仅 categoryName + borrowCount）. */
    public static class CategoryHotStat {
        @SerializedName("categoryName")
        private String categoryName;
        @SerializedName("borrowCount")
        private long borrowCount;

        public String getCategoryName() { return categoryName; }
        public long getBorrowCount() { return borrowCount; }
    }
}
