package com.library.ai.config;

import com.library.ai.llm.LlmUnavailableException;
import com.library.common.exception.ErrorCode;
import com.library.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * AI 模块异常处理器.
 * <p>
 * 处理来自 {@code library-ai} 模块的基础设施异常。
 * 与 {@code library-common} 中的 {@code GlobalExceptionHandler} 协同工作，
 * 优先匹配此处理器中的具体异常类型，未匹配的异常仍由全局兜底处理器处理。
 * <p>
 * 设计决策：此处理器置于 {@code library-ai} 而非 {@code library-common}，
 * 因为 {@code LlmUnavailableException} 定义在 AI 模块中，
 * 而 common 模块不应反向依赖 AI 模块。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class AiExceptionHandler {

    /**
     * LLM 服务不可用.
     * <p>
     * 当 DeepSeek API 不可达、超时、重试耗尽或 JSON 解析失败时，
     * 返回 HTTP 503 + {@link ErrorCode#LLM_UNAVAILABLE}，
     * 提示前端展示降级状态而非报错。
     */
    @ExceptionHandler(LlmUnavailableException.class)
    public ResponseEntity<Result<Void>> handleLlmUnavailable(
            LlmUnavailableException e, HttpServletRequest request) {
        log.warn("LLM 不可用: reason={}, message={}, path={}",
                e.getReason(), e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.error(ErrorCode.LLM_UNAVAILABLE));
    }
}
