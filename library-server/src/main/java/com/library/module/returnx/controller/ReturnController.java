package com.library.module.returnx.controller;

import com.library.common.result.Result;
import com.library.module.returnx.service.ReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "归还管理")
@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @Operation(summary = "归还图书")
    @PostMapping("/{borrowId}")
    public Result<?> returnBook(@PathVariable Long borrowId) {
        // TODO: 实现归还图书
        return Result.success();
    }
}
