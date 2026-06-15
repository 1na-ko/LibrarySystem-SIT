package com.library.module.acquisition.controller;
import com.library.common.result.Result;
import io.swagger.v3.oas.annotations.*; import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
@Tag(name="采购订单") @RestController @RequestMapping("/api/v1/acquisition/orders") @RequiredArgsConstructor
public class OrderController {
    @GetMapping public Result<?> list() { return Result.success("TODO"); }
    @PostMapping public Result<?> create(@RequestBody Object dto) { return Result.success("TODO"); }
    @PutMapping("/{id}/status") public Result<?> updateStatus(@PathVariable Long id, @RequestBody Object dto) { return Result.success("TODO"); }
}