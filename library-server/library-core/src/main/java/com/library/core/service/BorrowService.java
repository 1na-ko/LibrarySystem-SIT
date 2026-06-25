package com.library.core.service;

import com.library.common.result.PageResult;
import com.library.common.dto.PageDTO;
import com.library.core.vo.BorrowRecordVO;
import com.library.core.vo.BorrowResultVO;
import com.library.core.vo.RenewResultVO;

/**
 * 借阅管理服务接口.
 * <p>
 * 提供借书、还书、续借全生命周期的业务逻辑。
 * 所有方法以 {@code userId} 作为参数传入，由 Controller 层从 {@code SecurityContext} 提取。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public interface BorrowService {

    /**
     * 借书申请.
     * <p>
     * 校验链：用户状态 → 库存 → 借阅上限 → 重复借阅 → 超期未还 → Redis 锁 + 乐观锁扣库存.
     *
     * @param userId 用户 ID
     * @param bookId 图书 ID
     * @return 借阅结果（含 borrowId、书名、应还日期）
     * @throws com.library.common.exception.BizException 对应各业务错误码
     */
    BorrowResultVO borrow(Long userId, Long bookId);

    /**
     * 归还图书.
     * <p>
     * 校验借阅状态、归属当前用户，计算超期天数与罚款，恢复库存，发布 {@code BookReturnedEvent}.
     *
     * @param borrowId 借阅记录 ID
     * @param userId   当前用户 ID（用于归属校验）
     * @return 归还结果（含罚款金额）
     */
    BorrowRecordVO returnBook(Long borrowId, Long userId);

    /**
     * 续借图书.
     * <p>
     * 校验：续借次数 < 1、未超期、未被预约、归属当前用户。续借后 due_date 延长 30 天。
     *
     * @param borrowId 借阅记录 ID
     * @param userId   当前用户 ID（用于归属校验）
     * @return 续借结果（含新旧应还日期）
     * @throws com.library.common.exception.BizException 对应各业务错误码
     */
    RenewResultVO renew(Long borrowId, Long userId);

    /**
     * 查询当前用户的借阅列表（按状态可选筛选）.
     *
     * @param userId  用户 ID
     * @param status  状态筛选（可选：BORROWED / RENEWED / RETURNED / OVERDUE）
     * @param pageDTO 分页参数
     * @return 分页结果（嵌套 BookSimpleVO）
     */
    PageResult<BorrowRecordVO> getMyBorrows(Long userId, String status, PageDTO pageDTO);

    /**
     * 查询借阅详情（需校验归属）.
     *
     * @param borrowId 借阅记录 ID
     * @param userId   当前用户 ID
     * @return 借阅详情（嵌套 BookSimpleVO）
     * @throws com.library.common.exception.BizException 记录不存在或不属于当前用户
     */
    BorrowRecordVO getBorrowDetail(Long borrowId, Long userId);

    /**
     * 查询借阅历史（含 RETURNED/OVERDUE，支持按年份筛选）.
     *
     * @param userId  用户 ID
     * @param year    年份筛选（可选）
     * @param pageDTO 分页参数
     * @return 分页结果
     */
    PageResult<BorrowRecordVO> getHistory(Long userId, Integer year, PageDTO pageDTO);

    /**
     * 查询超期未还记录（管理员）.
     *
     * @param pageDTO 分页参数
     * @return 分页结果
     */
    PageResult<BorrowRecordVO> getOverdueRecords(PageDTO pageDTO);
}
