package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 错误响应 — 对应后端 ErrorResponse.
 */
public class ErrorResponse {

    @SerializedName("code")
    private int code;

    @SerializedName("message")
    private String message;

    @SerializedName("timestamp")
    private long timestamp;

    @SerializedName("path")
    private String path;

    @SerializedName("traceId")
    private String traceId;

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public String getPath() { return path; }
    public String getTraceId() { return traceId; }
}