package com.library.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * GlobalExceptionHandler 错误码→HTTP 映射单元测试.
 * <p>
 * 重点验证阶段 1 修复点：BAD_CREDENTIALS→401、ACCOUNT_FROZEN→403。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("GlobalExceptionHandler 错误码→HTTP 映射")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    // ==================== 阶段 1 修复点 ====================

    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"BAD_CREDENTIALS", "UNAUTHORIZED", "TOKEN_EXPIRED", "TOKEN_INVALID"})
    @DisplayName("认证类错误码应映射 401 UNAUTHORIZED")
    void shouldReturn401WhenAuthErrorCodes(ErrorCode code) {
        ResponseEntity<com.library.common.result.Result<Void>> resp =
                handler.handleBizException(new BizException(code), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getCode()).isEqualTo(code.getCode());
    }

    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"FORBIDDEN", "USER_DISABLED", "ACCOUNT_FROZEN"})
    @DisplayName("权限类错误码应映射 403 FORBIDDEN")
    void shouldReturn403WhenForbiddenErrorCodes(ErrorCode code) {
        ResponseEntity<com.library.common.result.Result<Void>> resp =
                handler.handleBizException(new BizException(code), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ==================== 既有映射回归 ====================

    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"NOT_FOUND", "BOOK_NOT_FOUND", "USER_NOT_FOUND"})
    @DisplayName("资源不存在类错误码应映射 404 NOT_FOUND")
    void shouldReturn404WhenNotFoundErrorCodes(ErrorCode code) {
        ResponseEntity<com.library.common.result.Result<Void>> resp =
                handler.handleBizException(new BizException(code), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest
    @EnumSource(value = ErrorCode.class, names = {"CONFLICT", "USERNAME_EXISTS", "ALREADY_BORROWED"})
    @DisplayName("冲突类错误码应映射 409 CONFLICT")
    void shouldReturn409WhenConflictErrorCodes(ErrorCode code) {
        ResponseEntity<com.library.common.result.Result<Void>> resp =
                handler.handleBizException(new BizException(code), request);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
