package com.library.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 借阅记录视图对象.
 * <p>
 * 嵌套 {@link BookSimpleVO} 便于前端直接展示借阅中图书信息。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRecordVO {

    /** 借阅记录 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 图书信息（嵌套精简视图） */
    private BookSimpleVO book;

    /** 借阅日期 */
    private LocalDate borrowDate;

    /** 应还日期 */
    private LocalDate dueDate;

    /** 实际归还日期 */
    private LocalDate returnDate;

    /** 续借次数 */
    private Integer renewCount;

    /** 状态（BORROWED / RENEWED / RETURNED / OVERDUE） */
    private String status;

    /** 罚款金额 */
    private BigDecimal fineAmount;
}
