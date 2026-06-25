package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 用户状态更新请求.
 */
public class UserStatusUpdateRequest {

    @SerializedName("status")
    private String status;

    public UserStatusUpdateRequest() {}

    public UserStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}