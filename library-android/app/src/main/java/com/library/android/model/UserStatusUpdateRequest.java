package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 用户状态变更请求体.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class UserStatusUpdateRequest {

    @SerializedName("status")
    private String status;

    public UserStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }
}
