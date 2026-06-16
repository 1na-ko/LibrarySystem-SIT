package com.library.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 借阅结果视图对象.
 * <p>
 * 对应 OpenAPI {@code BorrowResultVO} schema，借书成功时返回。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowResultVO {

    /** 借阅记录 ID */
    private Long borrowId;

    /** 书名 */
    private String bookTitle;

    /** 应还日期 */
    private LocalDate dueDate;

    /** 状态（固定为 BORROWED） */
    @Builder.Default
    private String status = "BORROWED";
}
