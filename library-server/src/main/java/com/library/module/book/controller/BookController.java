package com.library.module.book.controller;

import com.library.common.result.Result;
import com.library.module.book.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "图书管理")
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @Operation(summary = "获取图书列表")
    @GetMapping
    public Result<?> list() {
        // TODO: 实现图书列表查询
        return Result.success();
    }

    @Operation(summary = "获取图书详情")
    @GetMapping("/{id}")
    public Result<?> detail(@PathVariable Long id) {
        // TODO: 实现图书详情查询
        return Result.success();
    }

    @Operation(summary = "获取热门图书")
    @GetMapping("/hot")
    public Result<?> hot() {
        // TODO: 实现热门图书查询
        return Result.success();
    }

    @Operation(summary = "获取新书上架")
    @GetMapping("/new")
    public Result<?> newBooks() {
        // TODO: 实现新书上架查询
        return Result.success();
    }

    @Operation(summary = "新增图书")
    @PostMapping
    public Result<?> add() {
        // TODO: 实现新增图书
        return Result.success();
    }

    @Operation(summary = "更新图书信息")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id) {
        // TODO: 实现更新图书
        return Result.success();
    }

    @Operation(summary = "下架图书")
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        // TODO: 实现下架图书
        return Result.success();
    }
}
