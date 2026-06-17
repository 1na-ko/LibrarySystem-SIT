package com.library.security.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.annotation.OperationLog;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.core.entity.OperationLogEntity;
import com.library.core.mapper.OperationLogMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OperationLogAspect 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("OperationLogAspect 操作日志切面")
@ExtendWith(MockitoExtension.class)
class OperationLogAspectTest {

    @Mock
    private OperationLogMapper operationLogMapper;

    @Mock
    private ProceedingJoinPoint pjp;

    private OperationLogAspect aspect;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        aspect = new OperationLogAspect(operationLogMapper, objectMapper);
    }

    @Nested
    @DisplayName("字符串截断 truncate")
    class Truncate {

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(aspect.truncate(null, 10)).isNull();
        }

        @Test
        @DisplayName("短于限制的字符串应原样返回")
        void shouldReturnOriginalWhenUnderLimit() {
            assertThat(aspect.truncate("hello", 10)).isEqualTo("hello");
        }

        @Test
        @DisplayName("超长字符串应截断并追加...")
        void shouldTruncateLongString() {
            String result = aspect.truncate("123456789012345", 10);
            assertThat(result).hasSize(10).endsWith("...");
        }
    }

    @Nested
    @DisplayName("JSON 序列化 toJson")
    class ToJson {

        @Test
        @DisplayName("null 输入应返回 null")
        void shouldReturnNullForNullInput() {
            assertThat(aspect.toJson(null)).isNull();
        }

        @Test
        @DisplayName("对象应正确序列化为 JSON")
        void shouldSerializeObject() {
            String[] arr = {"a", "b"};
            assertThat(aspect.toJson(arr)).isEqualTo("[\"a\",\"b\"]");
        }
    }

    @Nested
    @DisplayName("客户端 IP 提取 extractClientIp")
    class ExtractClientIp {

        @Test
        @DisplayName("无 RequestContext 时应返回 unknown")
        void shouldReturnUnknownWhenNoRequestContext() {
            assertThat(aspect.extractClientIp()).isEqualTo("unknown");
        }
    }

    @Nested
    @DisplayName("异步写入 asyncInsert")
    class AsyncInsert {

        @Test
        @DisplayName("写入成功不应抛异常")
        void shouldNotThrowWhenInsertSucceeds() throws Exception {
            OperationLogEntity record = new OperationLogEntity();
            record.setModule("测试");
            record.setAction("测试");

            // asyncInsert 使用 CompletableFuture.runAsync — 等待异步完成
            aspect.asyncInsert(record);
            // 给异步任务一点时间执行
            Thread.sleep(200);

            verify(operationLogMapper).insert(record);
        }

        @Test
        @DisplayName("写入失败不应抛异常（不阻塞主流程）")
        void shouldNotThrowWhenInsertFails() throws Exception {
            OperationLogEntity record = new OperationLogEntity();
            doThrow(new RuntimeException("DB 不可用")).when(operationLogMapper).insert(any());

            assertThatCode(() -> {
                aspect.asyncInsert(record);
                Thread.sleep(200);
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("target 构建 buildTarget")
    class BuildTarget {

        @Test
        @DisplayName("无参数方法应返回 null")
        void shouldReturnNullWhenNoArgs() {
            assertThat(aspect.buildTarget(pjp, mockOperationLog())).isNull();
        }

        private OperationLog mockOperationLog() {
            return new OperationLog() {
                @Override
                public String module() { return "测试"; }
                @Override
                public String action() { return "测试"; }
                @Override
                public boolean logParams() { return true; }
                @Override
                public boolean logResult() { return false; }
                @Override
                public Class<? extends java.lang.annotation.Annotation> annotationType() {
                    return OperationLog.class;
                }
            };
        }
    }
}
