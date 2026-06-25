package com.library.security.controller;

import com.library.common.result.Result;
import com.library.core.enums.RoleEnum;
import com.library.core.service.StatsDashboardService;
import com.library.core.vo.DashboardVO;
import com.library.security.aspect.RequireRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员 — 流通统计 Controller.
 * <p>
 * 提供全局 Dashboard 统计（今日借阅/归还、本月趋势、热门分类等）。
 * 需要 LIBRARIAN 或 ADMIN 角色。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsDashboardService statsDashboardService;

    /**
     * 获取 Dashboard 统计.
     */
    @GetMapping("/dashboard")
    @RequireRole({RoleEnum.LIBRARIAN, RoleEnum.ADMIN})
    public Result<DashboardVO> dashboard() {
        return Result.success(statsDashboardService.getDashboard());
    }
}
