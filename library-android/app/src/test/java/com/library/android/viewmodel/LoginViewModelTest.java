package com.library.android.viewmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.library.android.model.LoginResponse;
import com.library.android.model.Result;
import com.library.android.model.UserProfile;
import com.library.android.network.TokenManager;
import com.library.android.repository.AuthRepository;
import com.library.android.testutil.ResultFactory;
import com.library.android.testutil.RxJava3SchedulerRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.reactivex.rxjava3.core.Single;

/**
 * LoginViewModel 单元测试 — P2-04.
 */
public class LoginViewModelTest {

    @Rule public InstantTaskExecutorRule instantTaskRule = new InstantTaskExecutorRule();
    @Rule public RxJava3SchedulerRule rxRule = new RxJava3SchedulerRule();

    private AuthRepository repository;
    private TokenManager tokenManager;
    private LoginViewModel viewModel;

    @Before
    public void setUp() {
        repository = mock(AuthRepository.class);
        tokenManager = mock(TokenManager.class);
        viewModel = new LoginViewModel(repository, tokenManager);
    }

    @Test
    public void login_emptyUsername_shouldPostErrorAndNotCallRepository() {
        viewModel.login("", "password");

        assertNotNull(viewModel.getErrorEvent().getValue());
        verify(repository, never()).login(anyString(), anyString());
    }

    @Test
    public void login_emptyPassword_shouldPostErrorAndNotCallRepository() {
        viewModel.login("user", "");

        assertNotNull(viewModel.getErrorEvent().getValue());
        verify(repository, never()).login(anyString(), anyString());
    }

    @Test
    public void login_success_shouldPersistTokensAndUserInfo() {
        UserProfile user = mock(UserProfile.class);
        when(user.getId()).thenReturn(42L);
        when(user.getRealName()).thenReturn("Alice");
        when(user.getRole()).thenReturn("STUDENT");

        LoginResponse resp = mock(LoginResponse.class);
        when(resp.getAccessToken()).thenReturn("at-1");
        when(resp.getRefreshToken()).thenReturn("rt-1");
        when(resp.getUser()).thenReturn(user);

        when(repository.login(anyString(), anyString()))
                .thenReturn(Single.just(ResultFactory.success(resp)));

        viewModel.login("alice", "pwd");

        verify(tokenManager, times(1)).saveTokens("at-1", "rt-1");
        verify(tokenManager, times(1)).saveUserInfo("alice", "Alice");
        verify(tokenManager, times(1)).saveUserRole("STUDENT");
        verify(tokenManager, times(1)).saveUserId(42L);
        assertTrue(Boolean.TRUE.equals(viewModel.isLoginSuccess().getValue()));
    }

    @Test
    public void login_successWithoutUser_shouldOnlySaveBasicInfo() {
        LoginResponse resp = mock(LoginResponse.class);
        when(resp.getAccessToken()).thenReturn("at");
        when(resp.getRefreshToken()).thenReturn("rt");
        when(resp.getUser()).thenReturn(null);

        when(repository.login(anyString(), anyString()))
                .thenReturn(Single.just(ResultFactory.success(resp)));

        viewModel.login("u", "p");

        verify(tokenManager).saveTokens("at", "rt");
        verify(tokenManager).saveUserInfo("u", null);
        verify(tokenManager, never()).saveUserRole(anyString());
        verify(tokenManager, never()).saveUserId(anyLong());
    }

    @Test
    public void login_apiFailure_shouldPostError() {
        when(repository.login(anyString(), anyString()))
                .thenReturn(Single.just(ResultFactory.failure(401, "用户名或密码错误")));

        viewModel.login("u", "p");

        Throwable err = viewModel.getErrorEvent().getValue();
        assertNotNull(err);
        assertEquals("用户名或密码错误", err.getMessage());
        assertNull(viewModel.isLoginSuccess().getValue());
        verify(tokenManager, never()).saveTokens(anyString(), anyString());
    }

    @Test
    public void login_networkException_shouldPostNetworkError() {
        when(repository.login(anyString(), anyString()))
                .thenReturn(Single.error(new java.io.IOException("connection reset")));

        viewModel.login("u", "p");

        Throwable err = viewModel.getErrorEvent().getValue();
        assertNotNull(err);
        assertTrue(err.getMessage().contains("网络错误"));
    }
}
