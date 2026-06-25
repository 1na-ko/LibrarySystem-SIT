package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.List;

/**
 * 借阅统计 VO（个人中心统计接口响应）.
 *
 * <p>WP-4 契约对齐：后端 UserStatsVO 字段类型为 Long/BigDecimal，
 * 原前端 int/double 在大数值/金融场景存在精度损失/溢出风险，已统一.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class BorrowStatsVO {

    @SerializedName("totalBorrows")
    private long totalBorrows;

    @SerializedName("currentBorrows")
    private long currentBorrows;

    @SerializedName("totalOverdue")
    private long totalOverdue;

    @SerializedName("totalFines")
    private BigDecimal totalFines;

    @SerializedName("categoryDistribution")
    private List<CategoryCount> categoryDistribution;

    @SerializedName("monthlyTrend")
    private List<MonthlyCount> monthlyTrend;

    public long getTotalBorrows() { return totalBorrows; }
    public void setTotalBorrows(long totalBorrows) { this.totalBorrows = totalBorrows; }

    public long getCurrentBorrows() { return currentBorrows; }
    public void setCurrentBorrows(long currentBorrows) { this.currentBorrows = currentBorrows; }

    public long getTotalOverdue() { return totalOverdue; }
    public void setTotalOverdue(long totalOverdue) { this.totalOverdue = totalOverdue; }

    public BigDecimal getTotalFines() { return totalFines; }
    public void setTotalFines(BigDecimal totalFines) { this.totalFines = totalFines; }

    /** 兼容旧调用：返回 double（存在精度损失，仅用于 UI 展示场景）. */
    public double getTotalFinesDouble() {
        return totalFines != null ? totalFines.doubleValue() : 0.0;
    }

    public List<CategoryCount> getCategoryDistribution() { return categoryDistribution; }
    public void setCategoryDistribution(List<CategoryCount> categoryDistribution) { this.categoryDistribution = categoryDistribution; }

    public List<MonthlyCount> getMonthlyTrend() { return monthlyTrend; }
    public void setMonthlyTrend(List<MonthlyCount> monthlyTrend) { this.monthlyTrend = monthlyTrend; }

    public static class CategoryCount {
        @SerializedName("categoryName")
        private String categoryName;
        @SerializedName("count")
        private long count;

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }

    public static class MonthlyCount {
        @SerializedName("month")
        private String month;
        @SerializedName("count")
        private long count;

        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }
}
