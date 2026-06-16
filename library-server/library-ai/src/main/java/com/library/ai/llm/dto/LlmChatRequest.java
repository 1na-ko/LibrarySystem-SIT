package com.library.ai.llm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DeepSeek Chat Completions 请求 DTO.
 * <p>
 * 遵循 OpenAI-compatible Chat Completions API 格式。
 * JSON Mode 下需设置 {@code responseFormat.type = "json_object"}。
 *
 * @author LibrarySystem Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmChatRequest {

    /** 模型名称，如 deepseek-chat */
    private String model;

    /** 对话消息列表 */
    private List<Message> messages;

    /** 采样温度 0.0-2.0 */
    private Double temperature;

    /** 最大生成 token 数 */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /** 是否流式输出（始终 false） */
    @Builder.Default
    private Boolean stream = false;

    /** 响应格式，JSON Mode 时设为 {"type": "json_object"} */
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    /**
     * 对话消息.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /** 角色：system / user / assistant */
        private String role;

        /** 消息内容 */
        private String content;
    }

    /**
     * 响应格式约束.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseFormat {
        /** "json_object" 或 "text" */
        private String type;
    }
}
