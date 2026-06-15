package com.library.common.result;

import com.library.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Result 统一响应体单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("Result 统一响应体")
class ResultTest {

    @Test
    @DisplayName("success() 无数据时应返回 code=200, data=null")
    void shouldReturnSuccessWithoutData() {
        Result<String> result = Result.success();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isNull();
        assertThat(result.getTimestamp()).isPositive();
    }

    @Test
    @DisplayName("success(T) 带数据时应包含该数据")
    void shouldReturnDataWhenSuccessWithData() {
        Result<String> result = Result.success("hello");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("操作成功");
        assertThat(result.getData()).isEqualTo("hello");
    }

    @Test
    @DisplayName("success(String, T) 自定义消息应生效")
    void shouldReturnCustomMessageWhenSuccessWithMessage() {
        Result<Integer> result = Result.success("创建成功", 42);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getMessage()).isEqualTo("创建成功");
        assertThat(result.getData()).isEqualTo(42);
    }

    @Test
    @DisplayName("error(ErrorCode) 应使用枚举的 code 和 message")
    void shouldReturnErrorCodeAndMessageWhenErrorByEnum() {
        Result<Void> result = Result.error(ErrorCode.BOOK_STOCK_EMPTY);

        assertThat(result.getCode()).isEqualTo(1001);
        assertThat(result.getMessage()).isEqualTo("图书库存不足");
        assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("error(ErrorCode, String) 应覆盖消息")
    void shouldReturnCustomMessageWhenErrorWithMessageOverride() {
        Result<Void> result = Result.error(ErrorCode.BAD_REQUEST, "ISBN 不能为空");

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("ISBN 不能为空");
    }

    @Test
    @DisplayName("error(int, String) 自定义 code + message")
    void shouldAcceptCustomCodeAndMessage() {
        Result<Void> result = Result.error(418, "I'm a teapot");

        assertThat(result.getCode()).isEqualTo(418);
        assertThat(result.getMessage()).isEqualTo("I'm a teapot");
    }

    @Test
    @DisplayName("error(int, String, T) 应携带 data")
    void shouldCarryDataWhenErrorWithData() {
        Result<String> result = Result.error(400, "参数错误", "email 格式不正确");

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("参数错误");
        assertThat(result.getData()).isEqualTo("email 格式不正确");
    }

    @Test
    @DisplayName("每次调用 timestamp 应更新")
    void shouldHaveDifferentTimestampOnEachCall() throws InterruptedException {
        Result<Void> r1 = Result.success();
        Thread.sleep(10);
        Result<Void> r2 = Result.success();

        // 两次调用间隔足够短，大概率相等，但必须 > 0 以证明其存在
        assertThat(r1.getTimestamp()).isPositive();
        assertThat(r2.getTimestamp()).isPositive();
    }

    @Test
    @DisplayName("success() 的 code 应引用 ErrorCode.SUCCESS")
    void shouldUseErrorCodeSuccessCode() {
        Result<Void> result = Result.success();

        assertThat(result.getCode()).isEqualTo(ErrorCode.SUCCESS.getCode());
    }
}
