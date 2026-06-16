package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 登录响应体.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class LoginResponse {

    @SerializedName("accessToken")
    private String accessToken;

    @SerializedName("refreshToken")
    private String refreshToken;

    @SerializedName("tokenType")
    private String tokenType;

    @SerializedName("expiresIn")
    private int expiresIn;

    @SerializedName("user")
    private UserProfile user;

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public int getExpiresIn() { return expiresIn; }
    public void setExpiresIn(int expiresIn) { this.expiresIn = expiresIn; }

    public UserProfile getUser() { return user; }
    public void setUser(UserProfile user) { this.user = user; }
}
