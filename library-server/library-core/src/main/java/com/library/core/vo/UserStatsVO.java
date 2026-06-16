package com.library.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 借阅统计视图对象.
 * <p>
 * 对应 OpenAPI {@code /users/me/stats} 响应的 data schema。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsVO {

    /** 累计借阅总数 */
    private Long totalBorrows;

    /** 当前在借数量 */
    private Long currentBorrows;

    /** 历史超期次数 */
    private Long totalOverdue;

    /** 累计罚款金额 */
    private BigDecimal totalFines;

    /** 分类借阅分布（饼图数据） */
    private List<CategoryStat> categoryDistribution;

    /** 近 12 月月度趋势 */
    private List<MonthlyStat> monthlyTrend;

    /**
     * 分类统计项.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStat {

        /** 分类名称 */
        private String categoryName;

        /** 借阅次数 */
        private Long count;
    }

    /**
     * 月度统计项.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyStat {

        /** 月份（格式：yyyy-MM） */
        private String month;

        /** 借阅次数 */
        private Long count;
    }
}
