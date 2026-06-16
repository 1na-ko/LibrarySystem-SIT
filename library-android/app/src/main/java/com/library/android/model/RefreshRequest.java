package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * Token 刷新请求体.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class RefreshRequest {

    @SerializedName("refreshToken")
    private String refreshToken;

    public RefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() { return refreshToken; }
}
