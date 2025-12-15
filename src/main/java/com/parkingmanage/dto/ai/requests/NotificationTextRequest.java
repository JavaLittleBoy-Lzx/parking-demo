package com.parkingmanage.dto.ai.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 通知文本生成请求
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationTextRequest {

    /**
     * 通知类型
     */
    private String notificationType;

    /**
     * 通知参数
     */
    private Object params;
}