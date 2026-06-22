package com.library.acquisition.vo;

import com.library.acquisition.enums.GapPriorityEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 馆藏缺口分析结果 VO（复本/热度语义）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GapAnalysisResultVO {
    private Long subjectId;
    private String subjectName;
    /** KG PageRank Top-N 核心书数 */
    private int totalCoreBooks;
    /** 核心书中馆藏已有的数量 */
    private int ownedBooks;
    /** coverage = ownedBooks / totalCoreBooks */
    private double coverage;
    private List<GapBook> gapBooks;
    /** WP-0：无核心书目数据等友好提示（非空时前端展示），区别于"暂无缺口". */
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GapBook {
        private Long bookId;
        private String isbn;
        private String title;
        private String author;
        /** 当前复本数 */
        private int currentCopies;
        /** 建议复本数 */
        private int suggestedCopies;
        /** 近12月借阅次数 */
        private long borrowCount;
        /** 热度比 borrowCount / (totalCopies * 12) */
        private double heatRatio;
        /** 周转率 */
        private double turnoverRate;
        private GapPriorityEnum priority;
        private String reason;
    }
}
