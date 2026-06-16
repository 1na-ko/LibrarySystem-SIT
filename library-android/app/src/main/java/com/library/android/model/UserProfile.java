package com.library.android.model;

import com.google.gson.annotations.SerializedName;

public class UserProfile {

    @SerializedName("id")
    private long id;

    @SerializedName("username")
    private String username;

    @SerializedName("realName")
    private String realName;

    @SerializedName("role")
    private String role;

    @SerializedName("email")
    private String email;

    @SerializedName("phone")
    private String phone;

    @SerializedName("maxBooks")
    private int maxBooks;

    @SerializedName("status")
    private String status;

    @SerializedName("createTime")
    private String createTime;

    public long getId() { return id; }
    public String getUsername() { return username; }
    public String getRealName() { return realName; }
    public String getRole() { return role; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public int getMaxBooks() { return maxBooks; }
    public String getStatus() { return status; }
    public String getCreateTime() { return createTime; }
}