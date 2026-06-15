package com.library.module.history.controller;

import com.library.common.result.Result;
import com.library.module.history.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "历史记录")
@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @Operation(summary = "历史记录分页列表")
    @GetMapping
    public Result<?> list() {
        // TODO: 实现历史记录分页列表
        return Result.success();
    }

    @Operation(summary = "历史记录统计")
    @GetMapping("/stats")
    public Result<?> stats() {
        // TODO: 实现历史记录统计
        return Result.success();
    }
}
