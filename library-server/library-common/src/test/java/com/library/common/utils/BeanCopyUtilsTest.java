package com.library.common.utils;

import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BeanCopyUtils 单元测试.
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@DisplayName("BeanCopyUtils Bean 拷贝")
class BeanCopyUtilsTest {

    @Data
    static class Source {
        private String name;
        private Integer age;
    }

    @Data
    static class Target {
        private String name;
        private Integer age;
        private String extra;
    }

    @Test
    @DisplayName("copy 应复制同名字段")
    void shouldCopyPropertiesWhenSameFields() {
        Source src = new Source();
        src.setName("alice");
        src.setAge(18);

        Target t = BeanCopyUtils.copy(src, Target.class);

        assertThat(t).isNotNull();
        assertThat(t.getName()).isEqualTo("alice");
        assertThat(t.getAge()).isEqualTo(18);
        assertThat(t.getExtra()).isNull();
    }

    @Test
    @DisplayName("copy null 源应返回 null")
    void shouldReturnNullWhenSourceNull() {
        assertThat(BeanCopyUtils.copy(null, Target.class)).isNull();
    }

    @Test
    @DisplayName("copyList 应批量复制且保持顺序")
    void shouldCopyListAndKeepOrder() {
        Source s1 = new Source();
        s1.setName("a");
        s1.setAge(1);
        Source s2 = new Source();
        s2.setName("b");
        s2.setAge(2);

        List<Target> result = BeanCopyUtils.copyList(List.of(s1, s2), Target.class);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("a");
        assertThat(result.get(1).getName()).isEqualTo("b");
    }

    @Test
    @DisplayName("copyList null 或空列表应返回空列表")
    void shouldReturnEmptyWhenNullOrEmpty() {
        assertThat(BeanCopyUtils.copyList(null, Target.class)).isEmpty();
        assertThat(BeanCopyUtils.copyList(Collections.emptyList(), Target.class)).isEmpty();
    }
}
