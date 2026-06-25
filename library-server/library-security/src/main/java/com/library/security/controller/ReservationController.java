package com.library.security.controller;

import com.library.common.dto.PageDTO;
import com.library.common.result.PageResult;
import com.library.common.result.Result;
import com.library.core.dto.ReservationRequest;
import com.library.core.enums.RoleEnum;
import com.library.core.service.ReservationService;
import com.library.core.vo.ReservationVO;
import com.library.security.aspect.RequireRole;
import com.library.security.context.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预约管理控制器.
 * <p>
 * 提供预约、取消、预约列表和排队位置查询端点。预约/取消限读者与管理员角色
 * （排除采编管理员，对齐权限矩阵 §2.3）；预约列表与排队位置（查询自身数据）对所有认证用户开放。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * 预约图书.
     */
    @RequireRole({RoleEnum.STUDENT, RoleEnum.TEACHER, RoleEnum.LIBRARIAN, RoleEnum.ADMIN})
    @PostMapping
    public Result<ReservationVO> reserve(@Valid @RequestBody ReservationRequest request) {
        long userId = SecurityUtils.getCurrentUserId();
        ReservationVO result = reservationService.reserve(userId, request.getBookId());
        return Result.success("预约成功", result);
    }

    /**
     * 取消预约.
     *
     * @param id 预约记录 ID
     */
    @RequireRole({RoleEnum.STUDENT, RoleEnum.TEACHER, RoleEnum.LIBRARIAN, RoleEnum.ADMIN})
    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        long userId = SecurityUtils.getCurrentUserId();
        reservationService.cancel(id, userId);
        return Result.<Void>success("取消成功", null);
    }

    /**
     * 我的预约列表.
     *
     * @param status 状态筛选（可选：WAITING / NOTIFIED / RESERVED / EXPIRED / COMPLETED / CANCELLED）
     */
    @GetMapping
    public Result<PageResult<ReservationVO>> getMyReservations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        long userId = SecurityUtils.getCurrentUserId();
        PageDTO pageDTO = new PageDTO(pageNum, pageSize);
        return Result.success(reservationService.getMyReservations(userId, status, pageDTO));
    }

    /**
     * 查询排队位置.
     *
     * @param id 预约记录 ID
     */
    @GetMapping("/{id}/queue-position")
    public Result<Integer> getQueuePosition(@PathVariable Long id) {
        long userId = SecurityUtils.getCurrentUserId();
        Integer position = reservationService.getQueuePosition(id, userId);
        return Result.success(position);
    }
}
