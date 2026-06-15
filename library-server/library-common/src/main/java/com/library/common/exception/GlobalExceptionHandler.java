package com.library.common.exception;

import com.library.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器.
 * <p>
 * 统一拦截各层抛出的异常，转换为 {@link Result} 响应，确保前端始终收到一致的错误格式。
 * <p>
 * 注意：Spring Security 的认证/授权异常（如 {@code AccessDeniedException}、
 * {@code AuthenticationException}）由 {@code library-security} 模块在
 * Spring Security 过滤器链中通过 {@code AuthenticationEntryPoint}
 * 和 {@code AccessDeniedHandler} 处理，不在本处理器中捕获。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常.
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("业务异常: code={}, message={}, path={}", e.getCode(), e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(mapHttpStatus(e.getErrorCode()))
                .body(Result.error(e.getCode(), e.getMessage()));
    }

    /**
     * 参数校验失败（@Valid / @Validated）.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}, path={}", msg, request.getRequestURI());
        return ResponseEntity
                .badRequest()
                .body(Result.error(ErrorCode.BAD_REQUEST.getCode(), msg));
    }

    /**
     * 运行时异常（兜底）.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknown(Exception e, HttpServletRequest request) {
        log.error("未捕获异常: path={}", request.getRequestURI(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(ErrorCode.INTERNAL_ERROR));
    }

    /**
     * 将业务错误码映射为 HTTP 状态码.
     */
    private HttpStatus mapHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED, TOKEN_EXPIRED, TOKEN_INVALID -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN, USER_DISABLED -> HttpStatus.FORBIDDEN;
            case NOT_FOUND, BOOK_NOT_FOUND, USER_NOT_FOUND,
                 SUPPLIER_NOT_FOUND, NEGOTIATION_NOT_FOUND,
                 RESERVATION_NOT_FOUND, BORROW_RECORD_NOT_FOUND,
                 KG_ENTITY_NOT_FOUND, ELECTRONIC_RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT, ALREADY_BORROWED, ALREADY_RESERVED,
                 BOOK_ALREADY_RETURNED, USERNAME_EXISTS, DUPLICATE_ISBN -> HttpStatus.CONFLICT;
            case LLM_UNAVAILABLE, KG_BUILD_FAILED, KG_NEO4J_UNAVAILABLE,
                 PREDICTION_DATA_INSUFFICIENT -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
