package com.library.module.reserve.controller;

import com.library.common.result.Result;
import com.library.module.reserve.service.ReserveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "预约管理")
@RestController
@RequestMapping("/api/v1/reserves")
@RequiredArgsConstructor
public class ReserveController {

    private final ReserveService reserveService;

    @Operation(summary = "预约图书")
    @PostMapping
    public Result<?> reserve() {
        // TODO: 实现预约图书
        return Result.success();
    }

    @Operation(summary = "取消预约")
    @DeleteMapping("/{id}")
    public Result<?> cancel(@PathVariable Long id) {
        // TODO: 实现取消预约
        return Result.success();
    }

    @Operation(summary = "我的预约列表")
    @GetMapping
    public Result<?> myReserves() {
        // TODO: 实现我的预约列表
        return Result.success();
    }

    @Operation(summary = "预约队列")
    @GetMapping("/queue/{bookId}")
    public Result<?> queue(@PathVariable Long bookId) {
        // TODO: 实现预约队列查询
        return Result.success();
    }
}
