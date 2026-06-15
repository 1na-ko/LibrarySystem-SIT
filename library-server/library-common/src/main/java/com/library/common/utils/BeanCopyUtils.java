package com.library.common.utils;

import cn.hutool.core.bean.BeanUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bean 属性拷贝工具类.
 * <p>
 * 基于 Hutool {@link BeanUtil#copyProperties(Object, Class)} 封装常用转换操作，
 * 提供单个对象拷贝与列表批量拷贝，适用于 Entity ↔ VO / Entity ↔ DTO 转换场景。
 * <p>
 * 注意：涉及复杂嵌套转换（如 Entity 树 → VO 树、多表聚合）时建议使用
 * {@code MapStruct} 接口编译期生成映射代码以获得更好的类型安全与可调试性。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BeanCopyUtils {

    /**
     * 拷贝单个对象.
     *
     * @param source 源对象
     * @param clazz  目标类型
     * @param <T>    目标泛型
     * @return 目标对象（source 为 null 时返回 null）
     */
    public static <T> T copy(Object source, Class<T> clazz) {
        if (source == null) {
            return null;
        }
        T target = BeanUtil.copyProperties(source, clazz);
        return target;
    }

    /**
     * 拷贝对象列表.
     *
     * @param sourceList 源列表
     * @param clazz      目标元素类型
     * @param <T>        目标泛型
     * @return 目标列表（sourceList 为 null 或空时返回空列表）
     */
    public static <T> List<T> copyList(List<?> sourceList, Class<T> clazz) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }
        return sourceList.stream()
                .map(source -> copy(source, clazz))
                .collect(Collectors.toList());
    }
}
