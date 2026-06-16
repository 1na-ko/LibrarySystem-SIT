package com.library.security.controller;

import com.library.common.dto.PageDTO;
import com.library.common.result.PageResult;
import com.library.common.result.Result;
import com.library.core.enums.RoleEnum;
import com.library.core.service.BorrowService;
import com.library.core.vo.BorrowRecordVO;
import com.library.security.aspect.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端-借阅控制器.
 * <p>
 * 仅 LIBRARIAN 和 ADMIN 可访问的借阅管理端点。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/borrows")
@RequiredArgsConstructor
public class AdminBorrowController {

    private final BorrowService borrowService;

    /**
     * 超期未还记录（管理员权限）.
     */
    @GetMapping("/overdue")
    @RequireRole({RoleEnum.LIBRARIAN, RoleEnum.ADMIN})
    public Result<PageResult<BorrowRecordVO>> getOverdue(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageDTO pageDTO = new PageDTO(pageNum, pageSize);
        return Result.success(borrowService.getOverdueRecords(pageDTO));
    }
}
