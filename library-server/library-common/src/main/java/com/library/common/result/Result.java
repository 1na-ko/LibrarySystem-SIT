package com.library.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.library.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体.
 * <p>
 * 所有 API 接口统一返回此结构，前端据此判断请求结果并进行统一的错误处理。
 * <pre>{@code
 * // 成功
 * Result.success(data);
 * // 失败
 * Result.error(ErrorCode.BOOK_STOCK_EMPTY);
 * }</pre>
 *
 * @param <T> 响应数据类型
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /** 状态码，200 表示成功 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 响应数据 */
    private T data;
    /** 响应时间戳（毫秒） */
    private long timestamp;

    // ==================== 静态工厂方法 ====================

    /** 操作成功（无返回数据） */
    public static <T> Result<T> success() {
        return build(ErrorCode.SUCCESS.getCode(), "操作成功", null);
    }

    /** 操作成功（带返回数据） */
    public static <T> Result<T> success(T data) {
        return build(ErrorCode.SUCCESS.getCode(), "操作成功", data);
    }

    /** 操作成功（自定义消息 + 数据） */
    public static <T> Result<T> success(String message, T data) {
        return build(ErrorCode.SUCCESS.getCode(), message, data);
    }

    /** 操作失败（使用 ErrorCode 枚举） */
    public static <T> Result<T> error(ErrorCode errorCode) {
        return build(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /** 操作失败（使用 ErrorCode 枚举 + 动态消息） */
    public static <T> Result<T> error(ErrorCode errorCode, String message) {
        return build(errorCode.getCode(), message, null);
    }

    /** 操作失败（自定义 code + message） */
    public static <T> Result<T> error(int code, String message) {
        return build(code, message, null);
    }

    /** 操作失败（自定义 code + message + 数据） */
    public static <T> Result<T> error(int code, String message, T data) {
        return build(code, message, data);
    }

    // ==================== 私有工具方法 ====================

    private static <T> Result<T> build(int code, String message, T data) {
        Result<T> result = new Result<>();
        result.code = code;
        result.message = message;
        result.data = data;
        result.timestamp = System.currentTimeMillis();
        return result;
    }
}
