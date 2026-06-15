package com.library.module.admin.controller;
import com.library.common.result.Result;
import io.swagger.v3.oas.annotations.*; import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*;
@Tag(name="管理后台") @RestController @RequestMapping("/api/v1/admin") @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    @GetMapping("/users") public Result<?> users() { return Result.success("TODO"); }
    @PutMapping("/users/{id}/role") public Result<?> changeRole(@PathVariable Long id, @RequestBody Object dto) { return Result.success("TODO"); }
    @PutMapping("/users/{id}/status") public Result<?> toggleStatus(@PathVariable Long id) { return Result.success("TODO"); }
    @GetMapping("/stats/overview") public Result<?> overview() { return Result.success("TODO"); }
    @GetMapping("/stats/borrow-trend") public Result<?> borrowTrend() { return Result.success("TODO"); }
    @GetMapping("/logs/operation") public Result<?> operationLogs() { return Result.success("TODO"); }
    @PostMapping("/es/reindex") public Result<?> reindex() { return Result.success("TODO"); }
}