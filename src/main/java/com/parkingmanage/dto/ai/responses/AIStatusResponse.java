package com.parkingmanage.dto.ai.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI功能状态响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIStatusResponse {
    /**
     * AI服务是否启用
     */
    private Boolean enabled;

    /**
     * API密钥是否配置
     */
    private Boolean apiKeyConfigured;

    /**
     * 各功能启用状态
     */
    private Map<String, Boolean> features;

    /**
     * 服务版本
     */
    private String version;

    /**
     * 状态消息
     */
    private String message;
}
