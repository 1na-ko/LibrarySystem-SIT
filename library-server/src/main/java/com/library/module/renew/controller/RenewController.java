package com.library.module.renew.controller;

import com.library.common.result.Result;
import com.library.module.renew.service.RenewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "续借管理")
@RestController
@RequestMapping("/api/v1/renews")
@RequiredArgsConstructor
public class RenewController {

    private final RenewService renewService;

    @Operation(summary = "续借图书")
    @PostMapping("/{borrowId}")
    public Result<?> renew(@PathVariable Long borrowId) {
        // TODO: 实现续借图书
        return Result.success();
    }
}
