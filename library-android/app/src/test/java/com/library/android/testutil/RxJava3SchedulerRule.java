package com.library.android.testutil;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import io.reactivex.rxjava3.android.plugins.RxAndroidPlugins;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * RxJava3 测试 Rule — 将 IO/Computation/Single/AndroidMain 全部 trampoline 化，
 * 让 ViewModel 在单元测试中同步流转 RxJava 调用，方便 LiveData 断言.
 *
 * <p>用法（与 {@code androidx.arch.core.testing.InstantTaskExecutorRule} 配合）：
 * <pre>{@code
 * @Rule public InstantTaskExecutorRule taskRule = new InstantTaskExecutorRule();
 * @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();
 * }</pre>
 */
public class RxJava3SchedulerRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                RxJavaPlugins.setIoSchedulerHandler(scheduler -> Schedulers.trampoline());
                RxJavaPlugins.setComputationSchedulerHandler(scheduler -> Schedulers.trampoline());
                RxJavaPlugins.setNewThreadSchedulerHandler(scheduler -> Schedulers.trampoline());
                RxJavaPlugins.setSingleSchedulerHandler(scheduler -> Schedulers.trampoline());
                RxAndroidPlugins.setMainThreadSchedulerHandler(scheduler -> Schedulers.trampoline());
                RxAndroidPlugins.setInitMainThreadSchedulerHandler(scheduler -> Schedulers.trampoline());
                try {
                    base.evaluate();
                } finally {
                    RxJavaPlugins.reset();
                    RxAndroidPlugins.reset();
                }
            }
        };
    }
}
