package com.library.module.ai.controller;
import com.library.common.result.Result;
import io.swagger.v3.oas.annotations.*; import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor; import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*; import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
@Tag(name="AI助手") @RestController @RequestMapping("/api/v1/ai") @RequiredArgsConstructor
public class AiChatController {
    @PostMapping("/chat") public Result<?> chat(@RequestBody Object dto) { return Result.success("TODO"); }
    @PostMapping(value="/chat/stream", produces=MediaType.TEXT_EVENT_STREAM_VALUE) public SseEmitter stream(@RequestBody Object dto) { SseEmitter e=new SseEmitter(120000L); e.complete(); return e; }
    @GetMapping("/sessions") public Result<?> sessions() { return Result.success("TODO"); }
    @GetMapping("/sessions/{id}") public Result<?> session(@PathVariable String id) { return Result.success("TODO"); }
    @DeleteMapping("/sessions/{id}") public Result<?> deleteSession(@PathVariable String id) { return Result.success("TODO"); }
}