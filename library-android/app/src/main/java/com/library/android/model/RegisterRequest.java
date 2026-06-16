package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 注册请求体.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
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

    public RegisterRequest(String username, String password, String realName, String email, String phone) {
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.email = email;
        this.phone = phone;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRealName() { return realName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
