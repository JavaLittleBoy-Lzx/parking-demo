package com.parkingmanage.dto.ai.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能问答响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntelligentQAResponse {
    /**
     * AI回答
     */
    private String answer;

    /**
     * 置信度
     */
    private Double confidence;

    /**
     * 参考文档
     */
    private String reference;

    /**
     * 响应时间
     */
    private Long timestamp;
}
