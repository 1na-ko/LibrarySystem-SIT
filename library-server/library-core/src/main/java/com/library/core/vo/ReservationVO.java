package com.library.core.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 预约记录视图对象.
 * <p>
 * 嵌套 {@link BookSimpleVO} 便于前端展示预约中图书信息。
 * {@code queuePosition} 表示当前排队位置，1 为队首。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationVO {

    /** 预约记录 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 图书信息（嵌套精简视图） */
    private BookSimpleVO book;

    /** 预约时间 */
    private LocalDateTime reserveTime;

    /** 通知时间 */
    private LocalDateTime notifyTime;

    /** 过期时间（通知后 48h） */
    private LocalDateTime expireTime;

    /** 状态（WAITING / NOTIFIED / RESERVED / EXPIRED / COMPLETED / CANCELLED） */
    private String status;

    /** 排队位置（1=队首） */
    private Integer queuePosition;
}
