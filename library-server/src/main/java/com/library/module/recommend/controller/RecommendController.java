package com.library.module.recommend.controller;
import com.library.common.result.Result;
import io.swagger.v3.oas.annotations.*; import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
@Tag(name="图书推荐") @RestController @RequestMapping("/api/v1/recommendations") @RequiredArgsConstructor
public class RecommendController {
    @GetMapping public Result<?> recommend() { return Result.success("TODO"); }
    @GetMapping("/related/{bookId}") public Result<?> related(@PathVariable Long bookId) { return Result.success("TODO"); }
    @GetMapping("/ai") public Result<?> aiRecommend() { return Result.success("TODO"); }
}