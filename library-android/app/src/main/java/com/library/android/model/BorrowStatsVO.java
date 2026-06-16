package com.library.android.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 借阅统计 VO（个人中心统计接口响应）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class BorrowStatsVO {

    @SerializedName("totalBorrows")
    private int totalBorrows;

    @SerializedName("currentBorrows")
    private int currentBorrows;

    @SerializedName("totalOverdue")
    private int totalOverdue;

    @SerializedName("totalFines")
    private double totalFines;

    @SerializedName("categoryDistribution")
    private List<CategoryCount> categoryDistribution;

    @SerializedName("monthlyTrend")
    private List<MonthlyCount> monthlyTrend;

    public int getTotalBorrows() { return totalBorrows; }
    public void setTotalBorrows(int totalBorrows) { this.totalBorrows = totalBorrows; }

    public int getCurrentBorrows() { return currentBorrows; }
    public void setCurrentBorrows(int currentBorrows) { this.currentBorrows = currentBorrows; }

    public int getTotalOverdue() { return totalOverdue; }
    public void setTotalOverdue(int totalOverdue) { this.totalOverdue = totalOverdue; }

    public double getTotalFines() { return totalFines; }
    public void setTotalFines(double totalFines) { this.totalFines = totalFines; }

    public List<CategoryCount> getCategoryDistribution() { return categoryDistribution; }
    public void setCategoryDistribution(List<CategoryCount> categoryDistribution) { this.categoryDistribution = categoryDistribution; }

    public List<MonthlyCount> getMonthlyTrend() { return monthlyTrend; }
    public void setMonthlyTrend(List<MonthlyCount> monthlyTrend) { this.monthlyTrend = monthlyTrend; }

    public static class CategoryCount {
        @SerializedName("categoryName")
        private String categoryName;
        @SerializedName("count")
        private int count;

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }

    public static class MonthlyCount {
        @SerializedName("month")
        private String month;
        @SerializedName("count")
        private int count;

        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
    }
}
