package com.parkingmanage.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * BigModel API请求体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BigModelRequest {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 生成结果的随机性，0-1
     */
    private Double temperature;

    /**
     * 生成结果的最大token数
     */
    private Integer maxTokens;

    /**
     * 影响输出文本的多样性
     */
    private Double topP;

    /**
     * 对话消息列表
     */
    private List<Message> messages;

    /**
     * 消息实体
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        /**
         * 角色：system, user, assistant
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;
    }
}