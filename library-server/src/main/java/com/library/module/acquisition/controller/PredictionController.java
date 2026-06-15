package com.library.module.acquisition.controller;
import com.library.common.result.Result;
import io.swagger.v3.oas.annotations.*; import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
@Tag(name="采购预测") @RestController @RequestMapping("/api/v1/acquisition/predictions") @RequiredArgsConstructor
public class PredictionController {
    @PostMapping("/generate") public Result<?> generate() { return Result.success("TODO"); }
    @GetMapping public Result<?> list() { return Result.success("TODO"); }
    @GetMapping("/{id}") public Result<?> detail(@PathVariable Long id) { return Result.success("TODO"); }
    @PutMapping("/{id}/approve") public Result<?> approve(@PathVariable Long id) { return Result.success("TODO"); }
}