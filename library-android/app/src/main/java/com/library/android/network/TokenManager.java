package com.library.android.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Token 管理器 — 优先使用 EncryptedSharedPreferences 安全存储 JWT 令牌对.
 *
 * <p>FQA-001 修复要点（2026-06-19，P0-01）：
 * 当设备 KeyStore 损坏 / AES256_GCM 不可用 / 加密 prefs 文件损坏（IOException）
 * 等异常场景下，原实现会抛 RuntimeException 导致应用启动直接崩溃。本类按以下三级降级：
 * <ol>
 *   <li>正常路径：MasterKeys + EncryptedSharedPreferences（生产场景）</li>
 *   <li>一级降级：删除疑似损坏的加密 prefs 文件 → 普通 SharedPreferences
 *       （文件名独立 {@code library_auth_prefs_fallback}，避免与残留加密文件命名冲突）</li>
 *   <li>极端兜底：普通 prefs 也失败 → 内存版 SharedPreferences（仅本会话有效）</li>
 * </ol>
 * 任何分支均保证 {@code getInstance()} 不抛异常，应用可继续启动；后续登录态可重建.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
public class TokenManager {

    private static final String TAG = "TokenManager";

    private static final String PREFS_NAME = "library_auth_prefs";
    private static final String FALLBACK_PREFS_NAME = "library_auth_prefs_fallback";

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_REAL_NAME = "real_name";
    private static final String KEY_USER_ID = "user_id";

    private static volatile TokenManager instance;

    private final SharedPreferences prefs;
    private final boolean encrypted;

    /**
     * SharedPreferences 工厂（package-private，仅供本类与单元测试）.
     * 抽象出 prefs 创建路径，便于在测试中注入失败行为.
     */
    @VisibleForTesting
    interface PrefsFactory {
        SharedPreferences create(@NonNull Context context)
                throws GeneralSecurityException, IOException;
    }

    /** 默认加密 prefs 工厂：MasterKeys + EncryptedSharedPreferences. */
    private static final PrefsFactory DEFAULT_ENCRYPTED_FACTORY = ctx -> {
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
        return EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                ctx,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    };

    /** 默认降级 prefs 工厂：普通 SharedPreferences（独立文件名避免冲突）. */
    private static final PrefsFactory DEFAULT_FALLBACK_FACTORY =
            ctx -> ctx.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE);

    private TokenManager(@NonNull Context context) {
        this(context, DEFAULT_ENCRYPTED_FACTORY, DEFAULT_FALLBACK_FACTORY);
    }

    /**
     * 受测试可见构造器，注入自定义 prefs 工厂以模拟加密/降级失败路径.
     * 生产代码请使用 {@link #getInstance(Context)}.
     */
    @VisibleForTesting
    TokenManager(@NonNull Context context,
                 @NonNull PrefsFactory encryptedFactory,
                 @NonNull PrefsFactory fallbackFactory) {
        SharedPreferences chosen;
        boolean isEncrypted;

        try {
            chosen = encryptedFactory.create(context);
            isEncrypted = true;
        } catch (Throwable encryptionError) {
            // 加密 prefs 不可用 — 记录告警并尝试清理疑似损坏的加密文件
            // 注：catch Throwable 覆盖：① 受检异常 GeneralSecurityException/IOException
            //                      ② 非受检异常 RuntimeException（KeyStore 内部 NPE 等）
            //                      ③ Error 子类如 ExceptionInInitializerError（部分定制 ROM
            //                         的 KeyGenParameterSpec.Builder 静态初始化失败）
            Log.w(TAG, "EncryptedSharedPreferences 初始化失败，将降级到普通 SharedPreferences",
                    encryptionError);
            deleteCorruptedPrefsFile(context, PREFS_NAME);

            try {
                chosen = fallbackFactory.create(context);
                isEncrypted = false;
            } catch (Throwable fallbackError) {
                // 普通 prefs 也失败属于设备级灾难（磁盘损坏 / 文件系统异常）
                // 极端兜底：使用 in-memory 实现保证应用不崩溃，本会话内仍可登录
                Log.e(TAG,
                        "降级 SharedPreferences 也失败，启用内存版 prefs（本会话内有效）",
                        fallbackError);
                chosen = new InMemorySharedPreferences();
                isEncrypted = false;
            }
        }

        this.prefs = chosen;
        this.encrypted = isEncrypted;
    }

    /** 删除可能损坏的加密 prefs 文件，避免下次启动重复失败. */
    private static void deleteCorruptedPrefsFile(@NonNull Context context,
                                                 @NonNull String prefsName) {
        try {
            File prefsDir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
            File prefsFile = new File(prefsDir, prefsName + ".xml");
            if (prefsFile.exists()) {
                boolean deleted = prefsFile.delete();
                Log.w(TAG, "清理疑似损坏的加密 prefs 文件 " + prefsName
                        + "，删除结果=" + deleted);
            }
        } catch (Exception e) {
            // 清理失败不是致命问题，仅记录
            Log.w(TAG, "清理加密 prefs 文件异常，忽略", e);
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

    /**
     * P1-04：异步预热实例，避免主线程首次注入时阻塞 ANR.
     *
     * <p>EncryptedSharedPreferences 首次创建涉及 AndroidKeyStore 交互（生成 / 获取 MasterKey）+
     * 加密 prefs 文件 I/O，最坏可达数百毫秒。Hilt 通过 OkHttp 拦截器链的依赖关系会在
     * MainActivity 启动早期同步触发 {@link #getInstance(Context)}，当冷启动 + 设备繁忙时
     * 可观测到主线程明显卡顿.
     *
     * <p>本方法在 {@link com.library.android.LibraryApplication#onCreate()} 中调用一次，
     * 通过后台守护线程触发首次实例化；后续主线程的 {@code getInstance()} 通过双检锁
     * 直接命中缓存（或在极小概率情况下短暂等待初始化线程释放 monitor）。
     *
     * <p>调用安全性：多次调用幂等（双检锁内幂等）；不会阻塞 caller.
     */
    public static void prewarm(@NonNull Context context) {
        if (instance != null) return;
        final Context appCtx = context.getApplicationContext();
        Thread t = new Thread(() -> {
            try {
                getInstance(appCtx);
            } catch (Throwable e) {
                Log.w(TAG, "TokenManager 后台预热异常（getInstance 已具备完整降级链，主线程后续调用仍安全）", e);
            }
        }, "TokenManager-Prewarm");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        t.start();
    }

    /** 仅供单元测试重置静态单例，禁止生产代码调用. */
    @VisibleForTesting
    static void resetForTesting() {
        synchronized (TokenManager.class) {
            instance = null;
        }
    }

    /** 标记当前 prefs 是否运行在加密模式下，便于诊断与监控. */
    @VisibleForTesting
    public boolean isEncrypted() {
        return encrypted;
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

    // ---------------------------------------------------------------------------------
    //  极端兜底：内存版 SharedPreferences（仅当加密 + 普通 prefs 均失败时启用）
    // ---------------------------------------------------------------------------------

    /**
     * SharedPreferences 内存实现 — 仅满足 TokenManager 当前调用语义.
     *
     * <p>仅用于"普通 SharedPreferences 也无法创建"的极端兜底（设备磁盘异常等），
     * 保证应用启动不崩溃；本会话登录状态可用，下次冷启动将丢失.
     */
    @VisibleForTesting
    static final class InMemorySharedPreferences implements SharedPreferences {

        private final ConcurrentMap<String, Object> data = new ConcurrentHashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(data);
        }

        @Nullable
        @Override
        public String getString(String key, @Nullable String defValue) {
            Object v = data.get(key);
            return v instanceof String ? (String) v : defValue;
        }

        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
            Object v = data.get(key);
            return v instanceof Set ? new HashSet<>((Set<String>) v) : defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object v = data.get(key);
            return v instanceof Integer ? (Integer) v : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object v = data.get(key);
            return v instanceof Long ? (Long) v : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object v = data.get(key);
            return v instanceof Float ? (Float) v : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object v = data.get(key);
            return v instanceof Boolean ? (Boolean) v : defValue;
        }

        @Override
        public boolean contains(String key) {
            return data.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new InMemoryEditor();
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
            // 内存版不支持监听
        }

        @Override
        public void unregisterOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) {
            // 内存版不支持监听
        }

        private final class InMemoryEditor implements Editor {

            private static final Object TOMBSTONE = new Object();
            private final Map<String, Object> pending = new HashMap<>();
            private boolean clearAll;

            @Override
            public Editor putString(String key, @Nullable String value) {
                pending.put(key, value == null ? TOMBSTONE : value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, @Nullable Set<String> values) {
                pending.put(key, values == null ? TOMBSTONE : new HashSet<>(values));
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                pending.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                pending.put(key, TOMBSTONE);
                return this;
            }

            @Override
            public Editor clear() {
                this.clearAll = true;
                return this;
            }

            @Override
            public boolean commit() {
                apply();
                return true;
            }

            @Override
            public void apply() {
                if (clearAll) {
                    data.clear();
                }
                for (Map.Entry<String, Object> entry : pending.entrySet()) {
                    if (entry.getValue() == TOMBSTONE) {
                        data.remove(entry.getKey());
                    } else {
                        data.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }
}
