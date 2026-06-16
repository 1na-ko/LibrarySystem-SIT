package com.library.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 罚款记录实体（对应 fine_record 表）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@TableName("fine_record")
public class FineRecord {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联借阅记录 ID */
    private Long borrowId;

    /** 罚款金额 */
    private BigDecimal amount;

    /** 罚款原因 */
    private String reason;

    /** 是否已缴（0=未缴, 1=已缴） */
    private Integer paid;

    /** 缴费日期 */
    private LocalDateTime paidDate;

    /** 逻辑删除（0=未删除, 1=已删除） */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
