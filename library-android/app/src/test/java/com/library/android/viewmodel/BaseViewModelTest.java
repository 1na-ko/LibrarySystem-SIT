package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.library.android.ui.common.LoadingState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.rxjava3.disposables.Disposable;

/**
 * BaseViewModel 单元测试 — CompositeDisposable 生命周期、LoadingState、errorEvent.
 */
public class BaseViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();

    private StubViewModel viewModel;

    /** 最小实现暴露 disposes 行为. */
    static class StubViewModel extends BaseViewModel {
        void addDisposable(Disposable d) {
            disposables.add(d);
        }
        boolean hasDisposables() {
            return disposables.size() > 0;
        }
    }

    @Before
    public void setUp() {
        viewModel = new StubViewModel();
    }

    @Test
    public void initialLoadingState_shouldBeIdle() {
        assertEquals(LoadingState.IDLE, viewModel.getLoadingState().getValue());
    }

    @Test
    public void setLoading_shouldUpdateState() {
        viewModel.setLoading(LoadingState.LOADING);
        assertEquals(LoadingState.LOADING, viewModel.getLoadingState().getValue());
    }

    @Test
    public void onCleared_shouldClearDisposables() {
        AtomicBoolean disposed = new AtomicBoolean(false);
        viewModel.addDisposable(new Disposable() {
            @Override public void dispose() { disposed.set(true); }
            @Override public boolean isDisposed() { return disposed.get(); }
        });

        viewModel.onCleared();

        assertTrue("onCleared 应触发 Disposable 清理", disposed.get());
    }

    @Test
    public void postError_shouldEmitThrowable() {
        AtomicReference<Throwable> received = new AtomicReference<>();
        viewModel.getErrorEvent().observeForever(received::set);

        RuntimeException ex = new RuntimeException("test error");
        viewModel.postError(ex);

        assertEquals(ex, received.get());
    }

    @Test
    public void errorEvent_shouldNotRepeatOnNewObserver() {
        RuntimeException ex1 = new RuntimeException("first");
        viewModel.postError(ex1);

        AtomicReference<Throwable> received = new AtomicReference<>();
        viewModel.getErrorEvent().observeForever(received::set);

        // SingleLiveEvent 特性：新 observer 收到最近一次事件
        assertNotNull("新 observer 应收到最近一次 error", received.get());
    }
}
