package com.library.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.library.core.enums.BorrowStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅记录实体（对应 borrow_record 表）.
 * <p>
 * 借阅状态由 {@link BorrowStatusEnum} 枚举控制，经由 MyBatis-Plus 默认
 * {@code MybatisEnumTypeHandler} 以 {@code name()} 与数据库 ENUM 值互转。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@TableName("borrow_record")
public class BorrowRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 图书 ID */
    private Long bookId;

    /** 借阅日期 */
    private LocalDate borrowDate;

    /** 应还日期 */
    private LocalDate dueDate;

    /** 实际归还日期 */
    private LocalDate returnDate;

    /** 续借次数（最多 1 次） */
    private Integer renewCount;

    /** 状态 */
    private BorrowStatusEnum status;

    /** 罚款金额 */
    private BigDecimal fineAmount;

    /** 逻辑删除（0=未删除, 1=已删除） */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
