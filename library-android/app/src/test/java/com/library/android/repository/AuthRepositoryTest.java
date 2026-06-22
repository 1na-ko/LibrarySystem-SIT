package com.library.android.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.library.android.model.LoginResponse;
import com.library.android.model.Result;
import com.library.android.network.exception.SessionExpiredException;
import com.library.android.network.exception.ValidationException;
import com.library.android.testutil.AbstractRepositoryTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.reactivex.rxjava3.observers.TestObserver;

public class AuthRepositoryTest extends AbstractRepositoryTest {

    private AuthRepository repository;

    @Before
    public void setUp() throws Exception {
        startServer();
        repository = new AuthRepository(api());
    }

    @After
    public void tearDown() throws Exception {
        shutdownServer();
    }

    @Test
    public void login_200_shouldReturnTokens() {
        enqueueJson(200, successData("{\"accessToken\":\"at-1\",\"refreshToken\":\"rt-1\","
                + "\"tokenType\":\"Bearer\",\"expiresIn\":7200,"
                + "\"user\":{\"id\":1,\"username\":\"alice\",\"realName\":\"Alice\",\"role\":\"STUDENT\"}}"));

        TestObserver<Result<LoginResponse>> obs = repository.login("alice", "pwd").test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> {
            assertTrue(r.isSuccess());
            assertEquals("at-1", r.getData().getAccessToken());
            assertEquals("rt-1", r.getData().getRefreshToken());
            assertNotNull(r.getData().getUser());
            assertEquals("STUDENT", r.getData().getUser().getRole());
            return true;
        });
    }

    @Test
    public void login_401_shouldEmitSessionExpiredException() {
        enqueueJson(401, "{\"code\":401,\"message\":\"用户名或密码错误\",\"data\":null}");

        TestObserver<Result<LoginResponse>> obs = repository.login("alice", "wrong").test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertError(SessionExpiredException.class);
    }

    @Test
    public void register_400_shouldEmitValidationException() {
        enqueueJson(400, "{\"code\":400,\"message\":\"邮箱格式不正确\",\"data\":null}");

        TestObserver<Result<LoginResponse>> obs =
                repository.register("u", "p", "n", "bad-email", "13800138000").test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertError(ValidationException.class);
    }

    @Test
    public void logout_200_shouldEmitSuccessResult() {
        enqueueJson(200, successNull());

        TestObserver<Result<Void>> obs = repository.logout().test();
        obs.awaitDone(2, java.util.concurrent.TimeUnit.SECONDS);
        obs.assertValue(r -> r.isSuccess());
    }
}
