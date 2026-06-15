package com.library.module.acquisition.controller;
import com.library.common.result.Result;
import io.swagger.v3.oas.annotations.*; import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
@Tag(name="合同管理") @RestController @RequestMapping("/api/v1/acquisition/contracts") @RequiredArgsConstructor
public class ContractController {
    @GetMapping public Result<?> list() { return Result.success("TODO"); }
    @GetMapping("/{id}") public Result<?> detail(@PathVariable Long id) { return Result.success("TODO"); }
    @PostMapping("/{id}/ai-analysis") public Result<?> aiAnalysis(@PathVariable Long id) { return Result.success("TODO"); }
}