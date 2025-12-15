package com.parkingmanage.dto.ai.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知文本生成响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTextResponse {
    /**
     * 生成的通知文本
     */
    private String text;

    /**
     * 通知类型
     */
    private String notificationType;

    /**
     * 生成时间
     */
    private Long timestamp;
}
