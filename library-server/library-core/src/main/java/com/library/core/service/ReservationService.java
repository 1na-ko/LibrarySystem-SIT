package com.library.core.service;

import com.library.common.result.PageResult;
import com.library.common.dto.PageDTO;
import com.library.core.vo.ReservationVO;

/**
 * 预约管理服务接口.
 * <p>
 * 当图书全部借出时，读者可预约排队；图书归还后自动通知排队首位读者。
 * 排队使用 Redis ZSET 按时间戳公平排序。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface ReservationService {

    /**
     * 预约图书.
     * <p>
     * 仅当库存为 0 时可预约；读者不可重复预约同一本书。
     *
     * @param userId 用户 ID
     * @param bookId 图书 ID
     * @return 预约结果（含排队位置）
     * @throws com.library.common.exception.BizException 有库存时抛 BOOK_AVAILABLE；重复时抛 ALREADY_RESERVED
     */
    ReservationVO reserve(Long userId, Long bookId);

    /**
     * 取消预约.
     * <p>
     * 仅允许取消本人状态为 WAITING 的预约。
     *
     * @param reservationId 预约记录 ID
     * @param userId        用户 ID
     * @throws com.library.common.exception.BizException 记录不存在或不可取消
     */
    void cancel(Long reservationId, Long userId);

    /**
     * 查询当前用户的预约列表（按状态可选筛选）.
     *
     * @param userId  用户 ID
     * @param status  状态筛选（可选）
     * @param pageDTO 分页参数
     * @return 分页结果（嵌套 BookSimpleVO，实时排队位置）
     */
    PageResult<ReservationVO> getMyReservations(Long userId, String status, PageDTO pageDTO);

    /**
     * 查询当前排队位置（从 Redis ZSET 实时获取）.
     *
     * @param reservationId 预约记录 ID
     * @param userId        当前登录用户 ID（用于归属校验，防横向越权）
     * @return 排队位置（1=队首），已不在队列时返回 null
     * @throws com.library.common.exception.BizException 记录不存在时抛 RESERVATION_NOT_FOUND；
     *         记录不属于当前用户时抛 FORBIDDEN
     */
    Integer getQueuePosition(Long reservationId, Long userId);
}
