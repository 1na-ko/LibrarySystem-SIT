package com.library.common.exception;

import com.library.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

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
     * 路径参数 / @RequestParam 约束校验失败（@Validated + @Min/@Max 等）.
     * <p>
     * Controller 类标注 {@code @Validated} 后，方法级参数约束（非 @RequestBody）校验失败
     * 时 Spring 抛出 {@link ConstraintViolationException}，而非 BindException 体系；
     * 若不单独处理将落入兜底返回 500，此处统一转 400。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(
            ConstraintViolationException e, HttpServletRequest request) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("约束校验失败: {}, path={}", msg, request.getRequestURI());
        return ResponseEntity
                .badRequest()
                .body(Result.error(ErrorCode.BAD_REQUEST.getCode(), msg));
    }

    /**
     * 不支持的 HTTP 方法（GET 接口收到 POST 请求等）.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("不支持的请求方法: {}, path={}", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.error(ErrorCode.BAD_REQUEST.getCode(), "不支持的请求方法"));
    }

    /**
     * 不支持的 Media Type（Content-Type）.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        log.warn("不支持的 Content-Type: {}, path={}", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(Result.error(ErrorCode.BAD_REQUEST.getCode(), "不支持的 Content-Type"));
    }

    /**
     * 参数类型转换失败（如 String→Long）.
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(
            TypeMismatchException e, HttpServletRequest request) {
        log.warn("参数类型转换失败: {}, path={}", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .badRequest()
                .body(Result.error(ErrorCode.BAD_REQUEST.getCode(), "参数类型不匹配"));
    }

    /**
     * 路径变量缺失.
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<Result<Void>> handleMissingPathVariable(
            MissingPathVariableException e, HttpServletRequest request) {
        log.warn("路径变量缺失: {}, path={}", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .badRequest()
                .body(Result.error(ErrorCode.BAD_REQUEST.getCode(), "路径变量缺失"));
    }

    /**
     * 数据完整性冲突（唯一约束违反、外键约束违反等）.
     * <p>
     * Spring 将 JDBC {@code SQLIntegrityConstraintViolationException} 等底层异常
     * 统一转换为 {@link DataIntegrityViolationException}。
     * 常见触发场景：并发借书/预约时突破 DB 唯一约束兜底、重复插入已存在记录等。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("数据完整性冲突: {}, path={}", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Result.error(ErrorCode.CONFLICT));
    }

    /**
     * 请求路径不存在（需配合 {@code spring.mvc.throw-exception-if-no-handler-found=true}）.
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNoHandler(
            NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("路径不存在: {}, path={}", e.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Result.error(ErrorCode.NOT_FOUND.getCode(), "请求路径不存在"));
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
     * 新增 ErrorCode 时<b>必须</b>同步更新此映射——本 switch 无 default 分支，遗漏任何枚举值将导致编译失败，强制保持完整。
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
                 PREDICTION_DATA_INSUFFICIENT,
                 RECOMMEND_PARALLEL_TIMEOUT -> HttpStatus.SERVICE_UNAVAILABLE;

            // SUCCESS 不会作为异常抛出；占位以保证 switch 覆盖全部枚举值（无 default，编译期强制完整）
            case SUCCESS -> HttpStatus.OK;
        };
    }
}
