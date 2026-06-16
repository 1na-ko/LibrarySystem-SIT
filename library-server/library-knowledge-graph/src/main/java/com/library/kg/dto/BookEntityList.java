package com.library.kg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * LLM JSON Mode 输出 — 图书实体识别结果.
 * <p>
 * 用于 DeepSeek API 结构化输出的反序列化，
 * 从图书标题/摘要/关键词中识别作者、关键词和学科实体。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookEntityList {

    /** 作者实体列表 */
    private List<Entity> authors;

    /** 关键词实体列表 */
    private List<Entity> keywords;

    /** 学科实体列表 */
    private List<Entity> subjects;

    /**
     * 单个命名实体.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entity {
        /** 实体名称（原始或规范化形式） */
        private String name;
        /** 置信度（0-1，LLM 输出） */
        private Double confidence;
    }
}
