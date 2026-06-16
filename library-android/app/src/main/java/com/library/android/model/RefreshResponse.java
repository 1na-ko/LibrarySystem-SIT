package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * Token 刷新响应体.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class RefreshResponse {

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;

    @SerializedName("expiresIn")
    private int expiresIn;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public int getExpiresIn() { return expiresIn; }
    public void setExpiresIn(int expiresIn) { this.expiresIn = expiresIn; }
}
