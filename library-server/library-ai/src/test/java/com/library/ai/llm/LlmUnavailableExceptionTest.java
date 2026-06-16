package com.library.ai.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LlmUnavailableException} 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("LlmUnavailableException")
class LlmUnavailableExceptionTest {

    @Nested
    @DisplayName("构造与字段")
    class Construction {

        @Test
        @DisplayName("含 reason 构造时应正确存储原因")
        void shouldStoreReasonWhenConstructedWithReason() {
            LlmUnavailableException ex = new LlmUnavailableException("API Key 无效", "AUTH_FAILED");

            assertThat(ex.getReason()).isEqualTo("AUTH_FAILED");
            assertThat(ex.getMessage()).isEqualTo("API Key 无效");
        }

        @Test
        @DisplayName("含 cause 构造时应保留原始异常")
        void shouldPreserveCauseWhenConstructedWithCause() {
            RuntimeException cause = new RuntimeException("连接超时");
            LlmUnavailableException ex = new LlmUnavailableException("DeepSeek API 网络错误", cause);

            assertThat(ex.getCause()).isSameAs(cause);
            assertThat(ex.getMessage()).isEqualTo("DeepSeek API 网络错误");
        }

        @Test
        @DisplayName("无参 reason 构造时默认 reason 应为 UNKNOWN")
        void shouldDefaultReasonToUnknownWhenNotSpecified() {
            LlmUnavailableException ex = new LlmUnavailableException("未知错误");

            assertThat(ex.getReason()).isEqualTo("UNKNOWN");
            assertThat(ex).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("继承关系")
    class Inheritance {

        @Test
        @DisplayName("应是 RuntimeException 的子类")
        void shouldBeInstanceOfRuntimeException() {
            LlmUnavailableException ex = new LlmUnavailableException("test");

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("含 cause 和 reason 的构造应同时保留两者")
        void shouldPreserveBothCauseAndReason() {
            RuntimeException cause = new RuntimeException("网络断开");
            LlmUnavailableException ex = new LlmUnavailableException(
                    "重试 2 次后仍失败", cause, "RETRY_EXHAUSTED");

            assertThat(ex.getCause()).isSameAs(cause);
            assertThat(ex.getReason()).isEqualTo("RETRY_EXHAUSTED");
            assertThat(ex.getMessage()).isEqualTo("重试 2 次后仍失败");
        }
    }
}
