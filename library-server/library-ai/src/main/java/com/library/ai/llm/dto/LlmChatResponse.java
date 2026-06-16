package com.library.ai.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DeepSeek Chat Completions 响应 DTO.
 * <p>
 * 对应 OpenAI-compatible Chat Completions API 的响应结构。
 * 通过 {@link #firstContent()} 便捷提取首个 choice 的文本内容。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatResponse {

    /** 请求唯一标识 */
    private String id;

    /** 生成结果列表 */
    private List<Choice> choices;

    /** Token 用量统计 */
    private Usage usage;

    /**
     * 生成选项.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        /** 选项序号 */
        private int index;

        /** 生成消息 */
        private Message message;

        /** 终止原因：stop / length / content_filter */
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * 消息体.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /** 角色 */
        private String role;

        /** 消息内容 */
        private String content;
    }

    /**
     * Token 用量.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        /** 提示词 token 数 */
        @JsonProperty("prompt_tokens")
        private int promptTokens;

        /** 生成 token 数 */
        @JsonProperty("completion_tokens")
        private int completionTokens;

        /** 总计 token 数 */
        @JsonProperty("total_tokens")
        private int totalTokens;
    }

    /**
     * 提取第一个 choice 的文本内容.
     *
     * @return 文本内容，无 choices 时返回空字符串
     */
    public String firstContent() {
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        Message msg = choices.get(0).getMessage();
        return msg != null && msg.getContent() != null ? msg.getContent() : "";
    }
}
