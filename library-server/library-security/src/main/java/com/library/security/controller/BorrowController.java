package com.library.security.controller;

import com.library.common.dto.PageDTO;
import com.library.common.result.PageResult;
import com.library.common.result.Result;
import com.library.core.dto.BorrowRequest;
import com.library.core.service.BorrowService;
import com.library.core.vo.BorrowRecordVO;
import com.library.core.vo.BorrowResultVO;
import com.library.core.vo.RenewResultVO;
import com.library.security.context.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 借阅管理控制器.
 * <p>
 * 提供借书、还书、续借、借阅列表和详情端点。所有认证用户均可访问自己的借阅数据。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    /**
     * 借书申请.
     */
    @PostMapping
    public Result<BorrowResultVO> borrow(@Valid @RequestBody BorrowRequest request) {
        long userId = SecurityUtils.getCurrentUserId();
        BorrowResultVO result = borrowService.borrow(userId, request.getBookId());
        return Result.success("借阅成功", result);
    }

    /**
     * 归还图书.
     *
     * @param id 借阅记录 ID
     */
    @PutMapping("/{id}/return")
    public Result<BorrowRecordVO> returnBook(@PathVariable Long id) {
        long userId = SecurityUtils.getCurrentUserId();
        BorrowRecordVO result = borrowService.returnBook(id, userId);
        return Result.success("归还成功", result);
    }

    /**
     * 续借图书.
     *
     * @param id 借阅记录 ID
     */
    @PutMapping("/{id}/renew")
    public Result<RenewResultVO> renew(@PathVariable Long id) {
        long userId = SecurityUtils.getCurrentUserId();
        RenewResultVO result = borrowService.renew(id, userId);
        return Result.success("续借成功", result);
    }

    /**
     * 我的借阅列表.
     *
     * @param status 状态筛选（可选：BORROWED / RENEWED / RETURNED / OVERDUE）
     */
    @GetMapping
    public Result<PageResult<BorrowRecordVO>> getMyBorrows(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        long userId = SecurityUtils.getCurrentUserId();
        PageDTO pageDTO = new PageDTO(pageNum, pageSize);
        return Result.success(borrowService.getMyBorrows(userId, status, pageDTO));
    }

    /**
     * 借阅详情.
     *
     * @param id 借阅记录 ID
     */
    @GetMapping("/{id}")
    public Result<BorrowRecordVO> getDetail(@PathVariable Long id) {
        long userId = SecurityUtils.getCurrentUserId();
        return Result.success(borrowService.getBorrowDetail(id, userId));
    }
}
