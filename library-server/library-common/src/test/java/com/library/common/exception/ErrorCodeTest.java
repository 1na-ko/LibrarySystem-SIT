package com.library.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ErrorCode 枚举单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("ErrorCode 枚举")
class ErrorCodeTest {

    @Test
    @DisplayName("所有错误码 code 值应唯一")
    void shouldAllCodesBeUnique() {
        Set<Integer> codes = new HashSet<>();
        for (ErrorCode ec : ErrorCode.values()) {
            assertThat(codes.add(ec.getCode()))
                    .as("错误码 %d (%s) 重复", ec.getCode(), ec.name())
                    .isTrue();
        }
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    @DisplayName("每个枚举值的 message 不应为空")
    void shouldHaveNonBlankMessageWhenValidCode(ErrorCode errorCode) {
        assertThat(errorCode.getMessage())
                .isNotBlank();
    }

    @Test
    @DisplayName("fromCode 应返回正确枚举值")
    void shouldReturnCorrectEnumWhenValidCode() {
        assertThat(ErrorCode.fromCode(200)).isEqualTo(ErrorCode.SUCCESS);
        assertThat(ErrorCode.fromCode(400)).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ErrorCode.fromCode(401)).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(ErrorCode.fromCode(403)).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(ErrorCode.fromCode(404)).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ErrorCode.fromCode(409)).isEqualTo(ErrorCode.CONFLICT);
        assertThat(ErrorCode.fromCode(500)).isEqualTo(ErrorCode.INTERNAL_ERROR);
    }

    @Test
    @DisplayName("fromCode 未匹配时应返回 INTERNAL_ERROR")
    void shouldReturnInternalErrorWhenUnknownCode() {
        assertThat(ErrorCode.fromCode(9999)).isEqualTo(ErrorCode.INTERNAL_ERROR);
        assertThat(ErrorCode.fromCode(-1)).isEqualTo(ErrorCode.INTERNAL_ERROR);
    }

    @Test
    @DisplayName("借阅域错误码应在 1000-1999 范围内")
    void shouldBeInCorrectRangeWhenBorrowDomain() {
        for (ErrorCode ec : ErrorCode.values()) {
            String name = ec.name();
            if (name.startsWith("BOOK_") || name.startsWith("BORROW_")
                    || name.startsWith("ALREADY_") || name.startsWith("RENEW_")
                    || name.startsWith("OVERDUE_") || name.startsWith("RESERVATION_")
                    || name.startsWith("ACCOUNT_")) {
                assertThat(ec.getCode()).isBetween(1000, 1999);
            }
        }
    }

    @Test
    @DisplayName("知识图谱域错误码应在 2000-2999 范围内")
    void shouldBeInCorrectRangeWhenKgDomain() {
        for (ErrorCode ec : ErrorCode.values()) {
            if (ec.name().startsWith("KG_")) {
                assertThat(ec.getCode()).isBetween(2000, 2999);
            }
        }
    }

    @Test
    @DisplayName("智能采编域错误码应在 3000-3999 范围内")
    void shouldBeInCorrectRangeWhenAcquisitionDomain() {
        for (ErrorCode ec : ErrorCode.values()) {
            if (ec.name().startsWith("PREDICTION_") || ec.name().startsWith("SUPPLIER_")
                    || ec.name().startsWith("NEGOTIATION_") || ec.name().startsWith("DUPLICATE_")
                    || ec.name().startsWith("ELECTRONIC_") || ec.name().startsWith("LLM_")) {
                assertThat(ec.getCode()).isBetween(3000, 3999);
            }
        }
    }

    @Test
    @DisplayName("认证域错误码应在 4000-4999 范围内")
    void shouldBeInCorrectRangeWhenAuthDomain() {
        for (ErrorCode ec : ErrorCode.values()) {
            if (ec.name().startsWith("USERNAME_") || ec.name().startsWith("USER_")
                    || ec.name().startsWith("BAD_CREDENTIALS") || ec.name().startsWith("TOKEN_")) {
                assertThat(ec.getCode()).isBetween(4000, 4999);
            }
        }
    }
}
