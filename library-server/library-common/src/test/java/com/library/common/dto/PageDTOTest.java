package com.library.common.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
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

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
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
    @DisplayName("构造器应对非法入参做钳制（pageNum<1→1, pageSize 越界→[1,100]）")
    void shouldClampInvalidValuesInConstructor() {
        PageDTO dto = new PageDTO(-1, 999999);

        assertThat(dto.getPageNum()).isEqualTo(1);
        assertThat(dto.getPageSize()).isEqualTo(100);
        // 钳制后值合法，Bean Validation 通过
        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    @DisplayName("构造器应对 pageSize=0 钳制为 1")
    void shouldClampZeroPageSizeInConstructor() {
        PageDTO dto = new PageDTO(1, 0);

        assertThat(dto.getPageSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("pageNum=0 时 @Min 注解校验应失败（setter 路径模拟 @Valid 绑定绕过构造器钳制）")
    void shouldFailValidationWhenPageNumLessThanOne() {
        PageDTO dto = new PageDTO();
        dto.setPageNum(0);

        var violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pageNum"));
    }

    @Test
    @DisplayName("pageSize=0 时 @Min 注解校验应失败（setter 路径）")
    void shouldFailValidationWhenPageSizeLessThanOne() {
        PageDTO dto = new PageDTO();
        dto.setPageSize(0);

        var violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("pageSize"));
    }

    @Test
    @DisplayName("pageSize=101 时 @Max 注解校验应失败（超过上限 100，setter 路径）")
    void shouldFailValidationWhenPageSizeExceedsMax() {
        PageDTO dto = new PageDTO();
        dto.setPageSize(101);

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
