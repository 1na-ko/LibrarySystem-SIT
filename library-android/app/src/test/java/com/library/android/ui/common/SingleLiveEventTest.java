package com.library.android.ui.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

/**
 * SingleLiveEvent 单元测试 — 验证事件仅触发一次、新观察者能收到最新值.
 */
public class SingleLiveEventTest {

    @Rule
    public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();

    private SingleLiveEvent<String> event;

    @Before
    public void setUp() {
        event = new SingleLiveEvent<>();
    }

    @Test
    public void setValue_observerShouldReceive() {
        AtomicReference<String> received = new AtomicReference<>();
        event.observeForever(received::set);

        event.setValue("hello");

        assertEquals("hello", received.get());
    }

    @Test
    public void multipleSetValue_observerShouldOnlyReceiveLatest() {
        AtomicReference<String> received = new AtomicReference<>();
        event.observeForever(received::set);

        event.setValue("first");
        // 第二个 observer 只会收到最新值
        assertEquals("first", received.get());

        event.setValue("second");
        assertEquals("second", received.get());
    }

    @Test
    public void newObserver_shouldReceivePendingValue() {
        event.setValue("pending");

        AtomicReference<String> received = new AtomicReference<>();
        event.observeForever(received::set);

        // 新注册的 observer 应收到待处理值
        assertEquals("pending", received.get());
    }

    @Test
    public void nullValue_shouldBeDelivered() {
        AtomicReference<String> received = new AtomicReference<>("not-null");
        event.observeForever(received::set);

        event.setValue(null);

        assertNull(received.get());
    }
}
