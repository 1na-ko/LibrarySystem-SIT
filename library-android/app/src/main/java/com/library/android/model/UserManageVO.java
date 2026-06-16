package com.library.android.model;

import com.google.gson.annotations.SerializedName;

/**
 * 用户管理 VO（管理员视图）.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class UserManageVO {

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

    @SerializedName("currentBorrows")
    private int currentBorrows;

    @SerializedName("totalOverdue")
    private int totalOverdue;

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

    public int getCurrentBorrows() { return currentBorrows; }
    public void setCurrentBorrows(int currentBorrows) { this.currentBorrows = currentBorrows; }

    public int getTotalOverdue() { return totalOverdue; }
    public void setTotalOverdue(int totalOverdue) { this.totalOverdue = totalOverdue; }

    public String getCreateTime() { return createTime; }
    public void setCreateTime(String createTime) { this.createTime = createTime; }

    public boolean isActive() { return "ACTIVE".equals(status); }
    public boolean isFrozen() { return "FROZEN".equals(status); }
    public boolean isDisabled() { return "DISABLED".equals(status); }
}
