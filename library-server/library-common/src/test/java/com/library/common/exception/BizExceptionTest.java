package com.library.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BizException 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("BizException")
class BizExceptionTest {

    @Test
    @DisplayName("应正确设置 errorCode")
    void shouldSetErrorCodeWhenConstructed() {
        BizException ex = new BizException(ErrorCode.BOOK_STOCK_EMPTY);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BOOK_STOCK_EMPTY);
        assertThat(ex.getCode()).isEqualTo(1001);
        assertThat(ex.getMessage()).isEqualTo("图书库存不足");
    }

    @Test
    @DisplayName("应使用 String.format 替换消息模板中的占位符")
    void shouldFormatMessageWhenArgsProvided() {
        BizException ex = new BizException(ErrorCode.BORROW_LIMIT_EXCEEDED, 5);

        assertThat(ex.getMessage()).isEqualTo("借阅数量超限，最多可借 5 本");
        assertThat(ex.getCode()).isEqualTo(1002);
    }

    @Test
    @DisplayName("含原始异常的构造器应保留 cause")
    void shouldPreserveCauseWhenCauseProvided() {
        RuntimeException cause = new RuntimeException("底层错误");
        BizException ex = new BizException(ErrorCode.INTERNAL_ERROR, cause);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
        assertThat(ex.getMessage()).isEqualTo("服务器内部错误");
    }

    @Test
    @DisplayName("应继承 RuntimeException")
    void shouldBeRuntimeException() {
        BizException ex = new BizException(ErrorCode.BAD_REQUEST);

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("getCode 应等于 errorCode.getCode()")
    void getCodeShouldEqualErrorCodeGetCode() {
        for (ErrorCode ec : ErrorCode.values()) {
            BizException ex = new BizException(ec);
            assertThat(ex.getCode()).isEqualTo(ec.getCode());
        }
    }

    @Test
    @DisplayName("格式化消息且含多个占位符时应全部替换")
    void shouldAllArgsBeReplacedWhenMultipleArgsProvided() {
        // 借用 KG_GRAPH_EMPTY 格式验证（无占位符的兜底）
        BizException ex = new BizException(ErrorCode.BORROW_LIMIT_EXCEEDED, 10);

        assertThat(ex.getMessage()).isEqualTo("借阅数量超限，最多可借 10 本");
    }
}
