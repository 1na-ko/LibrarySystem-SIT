package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * Token 刷新响应.
 */
public class RefreshResponse {

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;

    @SerializedName("expiresIn")
    private int expiresIn;

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public int getExpiresIn() { return expiresIn; }
}