package com.library.common.constraint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StrongPasswordValidator 密码强度校验单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("StrongPasswordValidator 密码强度校验")
class StrongPasswordValidatorTest {

    private StrongPasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StrongPasswordValidator();
        // initialize 接收 null annotation 即可（本实现不依赖注解属性）
        validator.initialize(null);
    }

    @Test
    @DisplayName("null 应视为有效（由 @NotBlank 负责非空校验）")
    void shouldReturnTrueWhenNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Nested
    @DisplayName("合法密码")
    class Valid {

        @ParameterizedTest
        @ValueSource(strings = {"Abc@1234", "Admin@123456", "Test_1234", "P@ssw0rd!"})
        @DisplayName("含大小写+数字+特殊字符且长度 8-32 应通过")
        void shouldPassWhenValidPassword(String pwd) {
            assertThat(validator.isValid(pwd, null)).isTrue();
        }

        @Test
        @DisplayName("长度恰好 32 位应通过")
        void shouldPassWhenLengthIs32() {
            // "aA1@" + 28 位数字 = 32 位
            assertThat(validator.isValid("aA1@5678901234567890123456789012", null)).isTrue();
        }
    }

    @Nested
    @DisplayName("非法密码")
    class Invalid {

        @ParameterizedTest
        @ValueSource(strings = {"Abc@123", "abc@1234", "ABC@1234", "Abc@abcd", "Abc12345", "aA1@"})
        @DisplayName("缺项或长度不足应失败")
        void shouldFailWhenInvalidPassword(String pwd) {
            assertThat(validator.isValid(pwd, null)).isFalse();
        }

        @Test
        @DisplayName("长度超过 32 位应失败")
        void shouldFailWhenTooLong() {
            String tooLong = "Ab@1" + "a".repeat(33);
            assertThat(validator.isValid(tooLong, null)).isFalse();
        }
    }
}
