package com.library.common.exception;

import lombok.Getter;

/**
 * 业务异常.
 * <p>
 * 各层在遇到业务规则不满足时抛出此异常，由 {@link GlobalExceptionHandler} 统一拦截
 * 并转换为 {@link com.library.common.result.Result} 响应。
 * <p>
 * 使用示例：
 * <pre>{@code
 * throw new BizException(ErrorCode.BOOK_STOCK_EMPTY);
 * throw new BizException(ErrorCode.BORROW_LIMIT_EXCEEDED, user.getMaxBooks());
 * }</pre>
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Getter
public class BizException extends RuntimeException {

    /** 错误码枚举 */
    private final ErrorCode errorCode;

    /**
     * 构造（无动态参数）.
     *
     * @param errorCode 错误码枚举
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 构造（含动态参数，用于 message 中的 String.format 占位符）.
     *
     * @param errorCode 错误码枚举
     * @param args 动态参数，替换 message 中的 %d/%s 等占位符
     */
    public BizException(ErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.errorCode = errorCode;
    }

    /**
     * 构造（含原始异常）.
     *
     * @param errorCode 错误码枚举
     * @param cause 原始异常
     */
    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    /** 获取业务错误码（int） */
    public int getCode() {
        return errorCode.getCode();
    }
}
