package com.library.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 流通统计 Dashboard VO.
 * <p>
 * 供管理员 Dashboard 展示：今日借阅/归还/超期数、
 * 本月日趋势、热门分类 Top-10、实时在馆人数。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardVO {

    /** 今日借阅数 */
    private long todayBorrows;
    /** 今日归还数 */
    private long todayReturns;
    /** 当前超期未还数 */
    private long todayOverdue;
    /** 实时在馆人数（当前活跃借阅人数） */
    private long activeBorrowers;
    /** 本月每日借阅/归还/超期趋势 */
    private List<DailyTrend> monthTrend;
    /** 热门分类 Top-10 */
    private List<CategoryHotStat> hotCategories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTrend {
        private String date;
        private long borrows;
        private long returns;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryHotStat {
        private String categoryName;
        private long borrowCount;
    }
}
