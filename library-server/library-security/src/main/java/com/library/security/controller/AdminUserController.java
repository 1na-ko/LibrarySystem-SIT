package com.library.security.controller;

import com.library.common.annotation.OperationLog;
import com.library.common.result.PageResult;
import com.library.common.result.Result;
import com.library.core.dto.UserQueryDTO;
import com.library.core.dto.UserStatusUpdateDTO;
import com.library.core.enums.RoleEnum;
import com.library.core.service.AdminUserService;
import com.library.core.vo.UserManageVO;
import com.library.security.aspect.RequireRole;
import com.library.security.context.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员 — 用户管理 Controller.
 * <p>
 * 提供用户列表分页查询和状态变更功能。
 * 需要 LIBRARIAN 或 ADMIN 角色。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * 分页查询用户列表.
     *
     * @param role    角色筛选（可选）
     * @param status  状态筛选（可选）
     * @param keyword 用户名/姓名搜索（可选）
     * @param page    页码（默认 1）
     * @param size    每页大小（默认 20，上限 100）
     */
    @GetMapping
    @RequireRole({RoleEnum.LIBRARIAN, RoleEnum.ADMIN})
    public Result<PageResult<UserManageVO>> list(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        UserQueryDTO query = new UserQueryDTO(role, status, keyword, page, size);
        return Result.success(adminUserService.listUsers(query));
    }

    /**
     * 变更用户状态（冻结/解冻/禁用）.
     *
     * @param id  目标用户 ID
     * @param dto 目标状态
     */
    @PutMapping("/{id}/status")
    @RequireRole({RoleEnum.LIBRARIAN, RoleEnum.ADMIN})
    @OperationLog(module = "用户管理", action = "状态变更")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UserStatusUpdateDTO dto) {
        long operatorId = SecurityUtils.getCurrentUserId();
        adminUserService.updateStatus(operatorId, id, dto);
        return Result.success();
    }
}
