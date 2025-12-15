package com.parkingmanage.dto.ai.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI使用统计响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIUsageResponse {
    /**
     * 总调用次数
     */
    private Long totalCalls;

    /**
     * 今日调用次数
     */
    private Long todayCalls;

    /**
     * 各功能调用统计
     */
    private Map<String, Long> featureUsage;

    /**
     * 平均响应时间(ms)
     */
    private Double averageResponseTime;

    /**
     * 成功率
     */
    private Double successRate;

    /**
     * 统计时间
     */
    private Long timestamp;
}
