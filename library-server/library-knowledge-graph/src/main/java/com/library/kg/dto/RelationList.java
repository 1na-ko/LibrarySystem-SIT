package com.library.kg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * LLM JSON Mode 输出 — 关系抽取结果.
 * <p>
 * 用于 DeepSeek API 结构化输出的反序列化，
 * 判断已识别实体之间的关系类型及权重。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelationList {

    /** 关系列表 */
    private List<Relation> relations;

    /**
     * 单条关系.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Relation {
        /** 源实体名称 */
        private String source;
        /** 目标实体名称 */
        private String target;
        /** 关系类型（AUTHORED_BY, BELONGS_TO, HAS_KEYWORD 等） */
        private String type;
        /** 关系权重（0-1，LLM 评估） */
        private Double weight;
    }
}
