package com.library.android.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TokenManager 单元测试 — P0-01 加密失败降级路径覆盖.
 *
 * <p>覆盖范围：
 * <ol>
 *   <li>正常加密路径仍生效（isEncrypted == true）</li>
 *   <li>加密初始化抛 GeneralSecurityException → 降级 plain prefs（isEncrypted == false，不崩溃）</li>
 *   <li>加密初始化抛 IOException → 降级 plain prefs（不崩溃）</li>
 *   <li>加密初始化抛 RuntimeException（KeyStore 异常路径） → 降级 plain prefs（不崩溃）</li>
 *   <li>加密 + plain 同时失败 → 启用 InMemorySharedPreferences 兜底（不崩溃）</li>
 *   <li>降级后 saveTokens / getAccessToken / saveUserRole / saveUserId 等 API 仍可读写</li>
 * </ol>
 */
public class TokenManagerTest {

    private Context mockContext;
    private SharedPreferences mockEncryptedPrefs;
    private SharedPreferences mockFallbackPrefs;

    @Before
    public void setUp() {
        TokenManager.resetForTesting();
        mockContext = mock(Context.class);
        // 任何 deleteCorruptedPrefsFile 调用都需要 ApplicationInfo
        ApplicationInfo appInfo = new ApplicationInfo();
        appInfo.dataDir = System.getProperty("java.io.tmpdir", "/tmp");
        when(mockContext.getApplicationInfo()).thenReturn(appInfo);

        mockEncryptedPrefs = newStubPrefs();
        mockFallbackPrefs = newStubPrefs();
    }

    @After
    public void tearDown() {
        TokenManager.resetForTesting();
    }

    // ---------------------------------------------------------------------------------
    //  1. 正常路径 — 加密 prefs 创建成功
    // ---------------------------------------------------------------------------------
    @Test
    public void normalEncryptedInit_shouldUseEncryptedPrefs() {
        TokenManager.PrefsFactory encryptedFactory = ctx -> mockEncryptedPrefs;
        TokenManager.PrefsFactory fallbackFactory = ctx -> mockFallbackPrefs;

        TokenManager tm = new TokenManager(mockContext, encryptedFactory, fallbackFactory);

        assertTrue("加密 prefs 创建成功时应处于加密模式", tm.isEncrypted());

        tm.saveTokens("at-1", "rt-1");
        // 写入应作用于加密 prefs，而非 fallback
        assertEquals("at-1", mockEncryptedPrefs.getString("access_token", null));
        assertNull("fallback prefs 不应被写入", mockFallbackPrefs.getString("access_token", null));
    }

    // ---------------------------------------------------------------------------------
    //  2. 加密失败 GeneralSecurityException → 降级 plain prefs，不抛异常
    // ---------------------------------------------------------------------------------
    @Test
    public void encryptedInitFailsWithGeneralSecurityException_shouldFallbackToPlainPrefs() {
        TokenManager.PrefsFactory encryptedFactory = ctx -> {
            throw new GeneralSecurityException("simulated KeyStore corruption");
        };
        TokenManager.PrefsFactory fallbackFactory = ctx -> mockFallbackPrefs;

        TokenManager tm = new TokenManager(mockContext, encryptedFactory, fallbackFactory);

        assertFalse("加密失败后应处于非加密降级模式", tm.isEncrypted());

        // 写入降级 prefs 仍可正常工作
        tm.saveTokens("at-fb", "rt-fb");
        assertEquals("at-fb", tm.getAccessToken());
        assertEquals("rt-fb", tm.getRefreshToken());
    }

    // ---------------------------------------------------------------------------------
    //  3. 加密失败 IOException → 降级
    // ---------------------------------------------------------------------------------
    @Test
    public void encryptedInitFailsWithIOException_shouldFallback() {
        TokenManager.PrefsFactory encryptedFactory = ctx -> {
            throw new IOException("simulated prefs file corrupted");
        };
        TokenManager.PrefsFactory fallbackFactory = ctx -> mockFallbackPrefs;

        TokenManager tm = new TokenManager(mockContext, encryptedFactory, fallbackFactory);

        assertFalse(tm.isEncrypted());
        tm.saveUserRole("ADMIN");
        assertTrue(tm.isAdmin());
    }

    // ---------------------------------------------------------------------------------
    //  4. 加密失败 RuntimeException（KeyStore 内部 NPE 等） → 降级
    // ---------------------------------------------------------------------------------
    @Test
    public void encryptedInitFailsWithRuntimeException_shouldFallback() {
        TokenManager.PrefsFactory encryptedFactory = ctx -> {
            throw new RuntimeException("AndroidKeyStore is not available on this device");
        };
        TokenManager.PrefsFactory fallbackFactory = ctx -> mockFallbackPrefs;

        TokenManager tm = new TokenManager(mockContext, encryptedFactory, fallbackFactory);

        assertFalse(tm.isEncrypted());
        tm.saveUserId(42L);
        assertEquals(42L, tm.getUserId());
    }

    // ---------------------------------------------------------------------------------
    //  5. 加密 + plain 同时失败 → 启用 InMemorySharedPreferences 兜底，不崩溃
    // ---------------------------------------------------------------------------------
    @Test
    public void bothFactoriesFail_shouldUseInMemoryAndNotCrash() {
        TokenManager.PrefsFactory encryptedFactory = ctx -> {
            throw new GeneralSecurityException("encrypted boom");
        };
        TokenManager.PrefsFactory fallbackFactory = ctx -> {
            throw new IOException("plain prefs disk full");
        };

        // 关键断言：构造器不抛异常
        TokenManager tm = new TokenManager(mockContext, encryptedFactory, fallbackFactory);

        assertNotNull("即使两次降级失败也应返回可用实例", tm);
        assertFalse(tm.isEncrypted());

        // 内存版仍可正常读写（本会话内有效）
        tm.saveTokens("at-mem", "rt-mem");
        tm.saveUserInfo("alice", "Alice");
        tm.saveUserRole("LIBRARIAN");
        tm.saveUserId(7L);

        assertEquals("at-mem", tm.getAccessToken());
        assertEquals("rt-mem", tm.getRefreshToken());
        assertEquals("alice", tm.getUsername());
        assertEquals("Alice", tm.getRealName());
        assertEquals("LIBRARIAN", tm.getUserRole());
        assertEquals(7L, tm.getUserId());
        assertTrue(tm.isLibrarianOrAbove());
        assertFalse(tm.isAdmin());
        assertTrue(tm.isLoggedIn());

        tm.clear();
        assertNull("clear 后 access token 应被移除", tm.getAccessToken());
        assertEquals(-1L, tm.getUserId());
        assertFalse(tm.isLoggedIn());
    }

    // ---------------------------------------------------------------------------------
    //  6. fallback 路径 — 角色 / 登录态 API 行为正确
    // ---------------------------------------------------------------------------------
    @Test
    public void fallbackMode_roleAndLoginStateApisShouldWork() {
        TokenManager.PrefsFactory encryptedFactory = ctx -> {
            throw new GeneralSecurityException("boom");
        };
        TokenManager.PrefsFactory fallbackFactory = ctx -> mockFallbackPrefs;

        TokenManager tm = new TokenManager(mockContext, encryptedFactory, fallbackFactory);

        // 未登录时
        assertFalse(tm.isLoggedIn());
        assertNull(tm.getUserRole());
        assertFalse(tm.isAdmin());
        assertFalse(tm.isLibrarianOrAbove());
        assertFalse(tm.isAcquisitorOrAbove());
        assertEquals(-1L, tm.getUserId());

        // ACQUISITOR 角色
        tm.saveTokens("a", "r");
        tm.saveUserRole("ACQUISITOR");
        assertTrue(tm.isLoggedIn());
        assertFalse(tm.isAdmin());
        assertFalse(tm.isLibrarianOrAbove());
        assertTrue(tm.isAcquisitorOrAbove());
    }

    // ---------------------------------------------------------------------------------
    //  7. P1-04 prewarm — 不阻塞调用线程，最终能完成实例化
    // ---------------------------------------------------------------------------------
    @Test
    public void prewarm_shouldNotBlockCallerAndCompleteInBackground() throws InterruptedException {
        // 注意：prewarm 内部走默认工厂，会触发 Android API（EncryptedSharedPreferences/MasterKeys）
        // 在纯 JVM 单元测试中，returnDefaultValues=true 让这些 stub 方法返回 null，
        // 导致 EncryptedSharedPreferences.create 内部 NPE → 走降级链 → 最终 in-memory 兜底
        // 但 ApplicationInfo + dataDir 必须存在以避免 deleteCorruptedPrefsFile 抛 NPE
        long start = System.nanoTime();
        TokenManager.prewarm(mockContext);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        // 关键断言：prewarm 几乎瞬时返回（< 50ms 给一点 slack）
        assertTrue("prewarm 必须异步，不可阻塞 caller，但耗时 " + elapsedMs + "ms",
                elapsedMs < 50);

        // 等待后台线程完成（最多 2 秒）
        for (int i = 0; i < 200; i++) {
            // 通过反射偷看 instance 字段不优雅；直接 getInstance 触发同步等待
            // （若 prewarm 已完成，立即返回；否则 synchronized 会等）
            if (System.nanoTime() - start > 2_000_000_000L) break;
            Thread.sleep(10);
        }

        // 这里直接 getInstance 必须返回非 null
        assertNotNull(TokenManager.getInstance(mockContext));
    }

    // ---------------------------------------------------------------------------------
    //  辅助：实现一个最小可用的 SharedPreferences 桩对象（基于 HashMap）
    // ---------------------------------------------------------------------------------
    private static SharedPreferences newStubPrefs() {
        SharedPreferences prefs = mock(SharedPreferences.class);
        SharedPreferences.Editor editor = mock(SharedPreferences.Editor.class);
        Map<String, Object> store = new HashMap<>();
        AtomicInteger pendingClear = new AtomicInteger(0);
        Map<String, Object> pending = new HashMap<>();

        // putString
        when(editor.putString(anyString(), any())).thenAnswer((Answer<SharedPreferences.Editor>) inv -> {
            String k = inv.getArgument(0);
            String v = inv.getArgument(1);
            if (v == null) pending.put(k, REMOVE_TOMBSTONE);
            else pending.put(k, v);
            return editor;
        });
        when(editor.putLong(anyString(), org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer((Answer<SharedPreferences.Editor>) inv -> {
                    pending.put(inv.getArgument(0), (Long) inv.getArgument(1));
                    return editor;
                });
        when(editor.putInt(anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenAnswer((Answer<SharedPreferences.Editor>) inv -> {
                    pending.put(inv.getArgument(0), (Integer) inv.getArgument(1));
                    return editor;
                });
        when(editor.putBoolean(anyString(), org.mockito.ArgumentMatchers.anyBoolean()))
                .thenAnswer((Answer<SharedPreferences.Editor>) inv -> {
                    pending.put(inv.getArgument(0), (Boolean) inv.getArgument(1));
                    return editor;
                });
        when(editor.remove(anyString())).thenAnswer((Answer<SharedPreferences.Editor>) inv -> {
            pending.put(inv.getArgument(0), REMOVE_TOMBSTONE);
            return editor;
        });
        when(editor.clear()).thenAnswer((Answer<SharedPreferences.Editor>) inv -> {
            pendingClear.set(1);
            return editor;
        });

        Answer<Object> applyAnswer = inv -> {
            if (pendingClear.getAndSet(0) == 1) {
                store.clear();
            }
            for (Map.Entry<String, Object> e : pending.entrySet()) {
                if (e.getValue() == REMOVE_TOMBSTONE) {
                    store.remove(e.getKey());
                } else {
                    store.put(e.getKey(), e.getValue());
                }
            }
            pending.clear();
            return null;
        };
        org.mockito.Mockito.doAnswer(applyAnswer).when(editor).apply();
        when(editor.commit()).thenAnswer((Answer<Boolean>) inv -> {
            applyAnswer.answer(inv);
            return true;
        });

        when(prefs.edit()).thenReturn(editor);
        when(prefs.getString(anyString(), any())).thenAnswer((Answer<String>) inv -> {
            String k = inv.getArgument(0);
            String def = inv.getArgument(1);
            Object v = store.get(k);
            return v instanceof String ? (String) v : def;
        });
        when(prefs.getLong(anyString(), org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer((Answer<Long>) inv -> {
                    String k = inv.getArgument(0);
                    long def = inv.getArgument(1);
                    Object v = store.get(k);
                    return v instanceof Long ? (Long) v : def;
                });
        when(prefs.contains(anyString())).thenAnswer((Answer<Boolean>) inv ->
                store.containsKey(inv.<String>getArgument(0)));
        return prefs;
    }

    private static final Object REMOVE_TOMBSTONE = new Object();
}
