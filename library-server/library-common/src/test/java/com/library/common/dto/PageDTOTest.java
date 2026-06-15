package com.library.common.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageDTO 公共分页请求 DTO 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("PageDTO 分页请求")
class PageDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("默认构造器应设 pageNum=1, pageSize=20")
    void shouldSetDefaultsWhenNoArgsConstructor() {
        PageDTO dto = new PageDTO();

        assertThat(dto.getPageNum()).isEqualTo(1);
        assertThat(dto.getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("getOffset() pageNum=1, pageSize=20 时应返回 0")
    void shouldReturnZeroWhenFirstPage() {
        PageDTO dto = new PageDTO(1, 20);

        assertThat(dto.getOffset()).isZero();
    }

    @Test
    @DisplayName("getOffset() pageNum=3, pageSize=10 时应返回 20")
    void shouldReturnCorrectOffset() {
        PageDTO dto = new PageDTO(3, 10);

        assertThat(dto.getOffset()).isEqualTo(20);
    }

    @Test
    @DisplayName("pageNum=0 时校验应失败")
    void shouldFailValidationWhenPageNumLessThanOne() {
        PageDTO dto = new PageDTO(0, 20);

        var violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pageNum"));
    }

    @Test
    @DisplayName("pageSize=0 时校验应失败")
    void shouldFailValidationWhenPageSizeLessThanOne() {
        PageDTO dto = new PageDTO(1, 0);

        var violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pageSize"));
    }

    @Test
    @DisplayName("pageSize=101 时校验应失败（超过上限 100）")
    void shouldFailValidationWhenPageSizeExceedsMax() {
        PageDTO dto = new PageDTO(1, 101);

        var violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pageSize"));
    }

    @Test
    @DisplayName("正常值 pageNum=1, pageSize=100 应通过校验")
    void shouldPassValidationWhenValidValues() {
        PageDTO dto = new PageDTO(1, 100);

        var violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("pageSize=1 边界值应通过校验")
    void shouldPassValidationWhenPageSizeIsOne() {
        PageDTO dto = new PageDTO(5, 1);

        var violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }
}
