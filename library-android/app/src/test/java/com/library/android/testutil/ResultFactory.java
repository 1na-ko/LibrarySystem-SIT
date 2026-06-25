package com.library.android.testutil;

import com.library.android.model.Result;

/**
 * 测试用 Result 工厂 — 简化 success / failure / api error 三种典型构造.
 */
public final class ResultFactory {

    private ResultFactory() {}

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage("OK");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> failure(int code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static <T> Result<T> failure(String message) {
        return failure(500, message);
    }
}
