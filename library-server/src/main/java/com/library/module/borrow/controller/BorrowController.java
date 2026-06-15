package com.library.module.borrow.controller;

import com.library.common.result.Result;
import com.library.module.borrow.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "借阅管理")
@RestController
@RequestMapping("/api/v1/borrows")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @Operation(summary = "借阅图书")
    @PostMapping
    public Result<?> borrow() {
        // TODO: 实现借阅图书
        return Result.success();
    }

    @Operation(summary = "获取当前借阅列表")
    @GetMapping
    public Result<?> list() {
        // TODO: 实现当前借阅列表
        return Result.success();
    }

    @Operation(summary = "获取借阅详情")
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        // TODO: 实现借阅详情
        return Result.success();
    }

    @Operation(summary = "检查借阅资格")
    @GetMapping("/check")
    public Result<?> check() {
        // TODO: 实现借阅资格检查
        return Result.success();
    }
}
