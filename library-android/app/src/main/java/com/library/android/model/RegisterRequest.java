package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 注册请求.
 */
public class RegisterRequest {

    @SerializedName("username")
    private String username;

    @SerializedName("password")
    private String password;

    @SerializedName("realName")
    private String realName;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    public RegisterRequest() {}

    public RegisterRequest(String username, String password, String realName, String email, String phone) {
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.email = email;
        this.phone = phone;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}