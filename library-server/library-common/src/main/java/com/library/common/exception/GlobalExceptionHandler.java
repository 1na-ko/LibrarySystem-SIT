package com.library.common.exception;

import com.library.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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
     * 参数绑定校验失败（@Valid on GET query DTO / model attribute）.
     * <p>
     * Spring MVC 对非 @RequestBody 的 @Valid 校验抛出 BindException 而非 MethodArgumentNotValidException。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(
            BindException e, HttpServletRequest request) {
        String msg = e.getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数绑定校验失败: {}, path={}", msg, request.getRequestURI());
        return ResponseEntity
                .badRequest()
                .body(Result.error(ErrorCode.BAD_REQUEST.getCode(), msg));
    }

    /**
     * 缺少必需请求参数.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParam(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        log.warn("缺少必需参数: {}, path={}", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .badRequest()
                .body(Result.error(ErrorCode.BAD_REQUEST.getCode(), "缺少必需参数: " + e.getParameterName()));
    }

    /**
     * 请求体 JSON 解析失败（格式错误、类型不匹配）.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(
            HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("请求体解析失败: {}, path={}", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .badRequest()
                .body(Result.error(ErrorCode.BAD_REQUEST.getCode(), "请求体格式错误"));
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
     * <p>
     * 新增 ErrorCode 时<b>必须</b>同步更新此映射，避免错误码落入 default 分支返回 400。
     */
    private HttpStatus mapHttpStatus(ErrorCode errorCode) {
        return switch (errorCode) {
            // 400 — 请求参数不合法
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;

            // 401 — 认证失败 / 凭证过期
            case UNAUTHORIZED, TOKEN_EXPIRED, TOKEN_INVALID, BAD_CREDENTIALS -> HttpStatus.UNAUTHORIZED;

            // 403 — 权限不足 / 账户受限
            case FORBIDDEN, USER_DISABLED, ACCOUNT_FROZEN -> HttpStatus.FORBIDDEN;

            // 404 — 资源不存在
            case NOT_FOUND, BOOK_NOT_FOUND, USER_NOT_FOUND,
                 CATEGORY_NOT_FOUND,
                 SUPPLIER_NOT_FOUND, NEGOTIATION_NOT_FOUND,
                 RESERVATION_NOT_FOUND, BORROW_RECORD_NOT_FOUND,
                 KG_ENTITY_NOT_FOUND, KG_GRAPH_EMPTY,
                 ELECTRONIC_RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;

            // 409 — 业务冲突 / 规则限制
            case CONFLICT, ALREADY_BORROWED, ALREADY_RESERVED,
                 BOOK_ALREADY_RETURNED, USERNAME_EXISTS, DUPLICATE_ISBN,
                 BOOK_STOCK_EMPTY, BORROW_LIMIT_EXCEEDED, OVERDUE_UNRETURNED,
                 RENEW_LIMIT_EXCEEDED, RENEW_OVERDUE, RENEW_RESERVED,
                 BOOK_AVAILABLE, RESERVATION_EXPIRED -> HttpStatus.CONFLICT;

            // 429 — 限流
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;

            // 500 — 服务器内部错误
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;

            // 503 — 依赖服务不可用
            case LLM_UNAVAILABLE, KG_BUILD_FAILED, KG_NEO4J_UNAVAILABLE,
                 PREDICTION_DATA_INSUFFICIENT -> HttpStatus.SERVICE_UNAVAILABLE;

            // 未列出的错误码 → 500（保守处理，避免将服务端错误误报为客户端错误）
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
