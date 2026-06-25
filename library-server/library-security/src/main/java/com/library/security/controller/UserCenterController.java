package com.library.security.controller;

import com.library.common.dto.PageDTO;
import com.library.common.result.PageResult;
import com.library.common.result.Result;
import com.library.core.dto.UpdateUserDTO;
import com.library.core.service.BorrowService;
import com.library.core.service.UserService;
import com.library.core.service.UserStatsService;
import com.library.core.vo.BorrowRecordVO;
import com.library.core.vo.UserProfile;
import com.library.core.vo.UserStatsVO;
import com.library.security.context.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心控制器.
 * <p>
 * 提供个人信息、借阅历史、借阅统计端点。仅当前用户可查看自身数据。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserCenterController {

    private final UserService userService;
    private final BorrowService borrowService;
    private final UserStatsService userStatsService;

    /**
     * 获取个人信息.
     */
    @GetMapping("/me")
    public Result<UserProfile> getProfile() {
        long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userService.getProfile(userId));
    }

    /**
     * 更新个人信息.
     */
    @PutMapping("/me")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateUserDTO dto) {
        long userId = SecurityUtils.getCurrentUserId();
        userService.updateProfile(userId, dto);
        return Result.<Void>success("更新成功", null);
    }

    /**
     * 借阅历史（含 RETURNED/OVERDUE，支持按年份筛选）.
     */
    @GetMapping("/me/history")
    public Result<PageResult<BorrowRecordVO>> getHistory(
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        long userId = SecurityUtils.getCurrentUserId();
        PageDTO pageDTO = new PageDTO(pageNum, pageSize);
        return Result.success(borrowService.getHistory(userId, year, pageDTO));
    }

    /**
     * 借阅统计.
     * <p>
     * 包含：累计总数、当前在借、超期次数、罚款总额、分类分布、近 12 月趋势。
     */
    @GetMapping("/me/stats")
    public Result<UserStatsVO> getStats() {
        long userId = SecurityUtils.getCurrentUserId();
        return Result.success(userStatsService.getStats(userId));
    }
}
