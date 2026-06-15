package com.library.module.user.controller;
import com.library.common.result.Result;
import io.swagger.v3.oas.annotations.*; import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
@Tag(name="用户管理") @RestController @RequestMapping("/api/v1/users") @RequiredArgsConstructor
public class UserController {
    @GetMapping("/profile") public Result<?> profile() { return Result.success("TODO"); }
    @PutMapping("/profile") public Result<?> updateProfile(@RequestBody Object dto) { return Result.success("TODO"); }
}