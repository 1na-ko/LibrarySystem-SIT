package com.library.android.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * SessionManager 单元测试 — 会话失效广播、消费、跨线程传递.
 */
public class SessionManagerTest {

    @Rule
    public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();

    private SessionManager sessionManager;

    @Before
    public void setUp() {
        sessionManager = new SessionManager();
    }

    @Test
    public void initialValue_shouldBeFalse() {
        assertEquals(Boolean.FALSE, sessionManager.getSessionExpired().getValue());
    }

    @Test
    public void notifyExpired_shouldPostTrue() {
        sessionManager.notifyExpired();
        assertEquals(Boolean.TRUE, sessionManager.getSessionExpired().getValue());
    }

    @Test
    public void consume_shouldResetToFalse() {
        sessionManager.notifyExpired();
        assertEquals(Boolean.TRUE, sessionManager.getSessionExpired().getValue());

        sessionManager.consume();
        assertEquals(Boolean.FALSE, sessionManager.getSessionExpired().getValue());
    }

    @Test
    public void observer_shouldReceiveNotify() {
        AtomicBoolean received = new AtomicBoolean(false);
        sessionManager.getSessionExpired().observeForever(expired -> {
            if (Boolean.TRUE.equals(expired)) received.set(true);
        });

        sessionManager.notifyExpired();

        assertTrue("Observer 应收到 sessionExpired=true 事件", received.get());
    }

    @Test
    public void observer_shouldNotReceiveAfterConsume() {
        AtomicReference<Boolean> lastValue = new AtomicReference<>(null);
        sessionManager.getSessionExpired().observeForever(lastValue::set);

        sessionManager.notifyExpired();
        sessionManager.consume();
        sessionManager.notifyExpired();

        assertEquals("最终值应为 true（最后一次 notify）", Boolean.TRUE, lastValue.get());
    }

    @Test
    public void notifyFromBackgroundThread_shouldPropagateToMainThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean received = new AtomicBoolean(false);

        sessionManager.getSessionExpired().observeForever(expired -> {
            if (Boolean.TRUE.equals(expired)) {
                received.set(true);
                latch.countDown();
            }
        });

        // 后台线程触发 notify（验证 postValue 线程安全）
        new Thread(() -> sessionManager.notifyExpired()).start();

        boolean done = latch.await(2, TimeUnit.SECONDS);
        assertTrue("后台线程 notify 应被 Observer 收到", done);
        assertTrue(received.get());
    }
}
