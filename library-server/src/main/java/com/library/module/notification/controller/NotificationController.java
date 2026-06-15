package com.library.module.notification.controller;
import com.library.common.result.Result;
import io.swagger.v3.oas.annotations.*; import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
@Tag(name="通知中心") @RestController @RequestMapping("/api/v1/notifications") @RequiredArgsConstructor
public class NotificationController {
    @GetMapping public Result<?> list() { return Result.success("TODO"); }
    @GetMapping("/unread-count") public Result<?> unreadCount() { return Result.success(0); }
    @PutMapping("/{id}/read") public Result<?> markRead(@PathVariable Long id) { return Result.success("TODO"); }
    @PutMapping("/read-all") public Result<?> markAllRead() { return Result.success("TODO"); }
}