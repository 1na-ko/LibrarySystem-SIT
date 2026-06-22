package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.repository.AuthRepository;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.reactivex.rxjava3.core.Single;

public class RegisterViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private AuthRepository repository;
    private RegisterViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(AuthRepository.class);
        viewModel = new RegisterViewModel(repository);
    }

    @Test
    public void register_missingFields_shouldNotCallRepository() {
        viewModel.register("", "p", "name", "e@x", "13800138000");
        assertNotNull(viewModel.getErrorEvent().getValue());
        verify(repository, never()).register(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void register_success_shouldFlagRegisterSuccessTrue() {
        when(repository.register(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Single.just(ResultFactory.success(null)));

        viewModel.register("u", "p", "real", "e@x.com", "13800138000");

        assertTrue(Boolean.TRUE.equals(viewModel.isRegisterSuccess().getValue()));
    }

    @Test
    public void register_apiFailure_shouldPostErrorAndNotMarkSuccess() {
        when(repository.register(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Single.just(ResultFactory.failure(409, "用户名已存在")));

        viewModel.register("u", "p", "real", "e@x.com", "13800138000");

        assertNotNull(viewModel.getErrorEvent().getValue());
        assertEquals("用户名已存在", viewModel.getErrorEvent().getValue().getMessage());
        assertNull(viewModel.isRegisterSuccess().getValue());
    }

    @Test
    public void register_networkException_shouldPostError() {
        when(repository.register(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Single.error(new java.io.IOException("conn refused")));

        viewModel.register("u", "p", "real", "e@x.com", "13800138000");

        Throwable t = viewModel.getErrorEvent().getValue();
        assertNotNull(t);
        assertTrue(t.getMessage().contains("网络错误"));
    }
}
