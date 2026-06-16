package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 统一响应体包装类，对应后端 {@code Result<T>} 结构.
 *
 * @param <T> 响应数据类型
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class Result<T> {

    @SerializedName("code")
    private int code;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private T data;

    @SerializedName("timestamp")
    private long timestamp;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /** 请求是否成功（code == 200）. */
    public boolean isSuccess() {
        return code == 200;
    }
}
