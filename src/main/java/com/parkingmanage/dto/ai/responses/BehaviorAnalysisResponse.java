package com.parkingmanage.dto.ai.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户行为分析响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorAnalysisResponse {
    /**
     * 分析结果
     */
    private String analysis;

    /**
     * 风险等级 (low/medium/high)
     */
    private String riskLevel;

    /**
     * 建议操作
     */
    private String recommendation;

    /**
     * 分析时间
     */
    private Long timestamp;
}
