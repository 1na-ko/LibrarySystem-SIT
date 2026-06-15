package com.library.module.acquisition.controller;
import com.library.common.result.Result;
import io.swagger.v3.oas.annotations.*; import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
@Tag(name="查重查缺") @RestController @RequestMapping("/api/v1/acquisition") @RequiredArgsConstructor
public class DedupController {
    @PostMapping("/dedup/check") public Result<?> check(@RequestBody Object dto) { return Result.success("TODO"); }
    @PostMapping("/dedup/batch-check") public Result<?> batchCheck(@RequestBody Object dto) { return Result.success("TODO"); }
    @GetMapping("/gap-analysis") public Result<?> gapAnalysis() { return Result.success("TODO"); }
}