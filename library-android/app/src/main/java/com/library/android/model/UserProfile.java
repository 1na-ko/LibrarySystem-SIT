package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 用户信息 VO.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
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
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public int getMaxBooks() { return maxBooks; }
    public void setMaxBooks(int maxBooks) { this.maxBooks = maxBooks; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    /** 是否为管理员角色（可访问后台管理功能）. */
    public boolean isAdmin() {
        return "ADMIN".equals(role) || "LIBRARIAN".equals(role);
    }
}
