package com.library.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.library.core.enums.ReservationStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预约记录实体（对应 reservation 表）.
 * <p>
 * 预约状态由 {@link ReservationStatusEnum} 枚举控制。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@TableName("reservation")
public class Reservation {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 图书 ID */
    private Long bookId;

    /** 预约时间 */
    private LocalDateTime reserveTime;

    /** 通知时间 */
    private LocalDateTime notifyTime;

    /** 过期时间（通知后 48h） */
    private LocalDateTime expireTime;

    /** 状态 */
    private ReservationStatusEnum status;

    /** 排队序号 */
    private Integer queuePosition;

    /** 逻辑删除（0=未删除, 1=已删除） */
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
