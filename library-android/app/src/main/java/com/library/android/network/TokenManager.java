package com.library.android.network;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Token 管理器 — 使用 EncryptedSharedPreferences 安全存储 JWT 令牌对.
 *
 * <p>提供 Access Token / Refresh Token 的持久化读写，以及登录态判断。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class TokenManager {

    private static final String PREFS_NAME = "library_auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_REAL_NAME = "real_name";
    private static final String KEY_USER_ID = "user_id";

    private static volatile TokenManager instance;
    private final SharedPreferences prefs;

    private TokenManager(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            prefs = EncryptedSharedPreferences.create(
                    PREFS_NAME,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            // Fallback to plain SharedPreferences if encryption unavailable
            throw new RuntimeException("Failed to initialize encrypted prefs", e);
        }
    }

    public static TokenManager getInstance(Context context) {
        if (instance == null) {
            synchronized (TokenManager.class) {
                if (instance == null) {
                    instance = new TokenManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public void saveUserInfo(String username, String realName) {
        prefs.edit()
                .putString(KEY_USERNAME, username)
                .putString(KEY_REAL_NAME, realName)
                .apply();
    }

    @Nullable
    public String getUsername() {
        return prefs.getString(KEY_USERNAME, null);
    }

    @Nullable
    public String getRealName() {
        return prefs.getString(KEY_REAL_NAME, null);
    }

    @Nullable
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    @Nullable
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public void saveUserRole(String role) {
        prefs.edit().putString(KEY_USER_ROLE, role).apply();
    }

    @Nullable
    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, null);
    }

    public void saveUserId(long userId) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply();
    }

    /** 获取用户 ID，未登录或未保存时返回 -1L. */
    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, -1L);
    }

    /** 是否为系统管理员（仅 ADMIN）. */
    public boolean isAdmin() {
        return "ADMIN".equals(getUserRole());
    }

    /** 是否具备图书馆员及以上权限（LIBRARIAN / ADMIN）. */
    public boolean isLibrarianOrAbove() {
        String role = getUserRole();
        return "ADMIN".equals(role) || "LIBRARIAN".equals(role);
    }

    /** 是否具备采编员权限（ACQUISITOR / LIBRARIAN / ADMIN）. */
    public boolean isAcquisitorOrAbove() {
        String role = getUserRole();
        return "ADMIN".equals(role) || "LIBRARIAN".equals(role) || "ACQUISITOR".equals(role);
    }

    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
