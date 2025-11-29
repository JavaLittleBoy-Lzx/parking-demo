package com.parkingmanage.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * BigModel API响应体
 */
@Data
public class BigModelResponse {

    /**
     * 请求ID
     */
    private String id;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 创建时间
     */
    private Long created;

    /**
     * 响应选择
     */
    private List<Choice> choices;

    /**
     * 使用统计
     */
    @JsonProperty("usage")
    private Usage usage;

    /**
     * 选择项
     */
    @Data
    public static class Choice {
        /**
         * 索引
         */
        private Integer index;

        /**
         * 消息
         */
        private Message message;

        /**
         * 完成原因
         */
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * 消息
     */
    @Data
    public static class Message {
        /**
         * 角色
         */
        private String role;

        /**
         * 内容
         */
        private String content;
    }

    /**
     * 使用统计
     */
    @Data
    public static class Usage {
        /**
         * 提示token数
         */
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;

        /**
         * 完成token数
         */
        @JsonProperty("completion_tokens")
        private Integer completionTokens;

        /**
         * 总token数
         */
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}