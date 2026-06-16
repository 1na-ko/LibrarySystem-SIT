package com.library.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 续借结果视图对象.
 * <p>
 * 对应 OpenAPI {@code RenewResultVO} schema，续借成功时返回。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewResultVO {

    /** 借阅记录 ID */
    private Long borrowId;

    /** 续借前应还日期 */
    private LocalDate oldDueDate;

    /** 续借后应还日期（延长 30 天） */
    private LocalDate newDueDate;

    /** 续借次数 */
    private Integer renewCount;
}
