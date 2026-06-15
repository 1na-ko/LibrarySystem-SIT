package com.library.module.graph.controller;
import com.library.common.result.Result;
import io.swagger.v3.oas.annotations.*; import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*;
@Tag(name="知识图谱") @RestController @RequestMapping("/api/v1/graph") @RequiredArgsConstructor
public class GraphController {
    @GetMapping("/book/{bookId}/relations") public Result<?> bookRelations(@PathVariable Long bookId) { return Result.success("TODO"); }
    @GetMapping("/book/{bookId}/trace") public Result<?> bookTrace(@PathVariable Long bookId) { return Result.success("TODO"); }
    @GetMapping("/author/{name}/network") public Result<?> authorNetwork(@PathVariable String name) { return Result.success("TODO"); }
    @GetMapping("/keyword/{word}/cloud") public Result<?> keywordCloud(@PathVariable String word) { return Result.success("TODO"); }
    @GetMapping("/category/{code}/overview") public Result<?> categoryOverview(@PathVariable String code) { return Result.success("TODO"); }
    @GetMapping("/hotspot") public Result<?> hotspot() { return Result.success("TODO"); }
    @GetMapping("/search") public Result<?> search(@RequestParam String q) { return Result.success("TODO"); }
}