package com.parkingmanage.dto.ai.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据报告生成响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataReportResponse {
    /**
     * 生成的报告内容
     */
    private String report;

    /**
     * 报告类型
     */
    private String reportType;

    /**
     * 生成时间
     */
    private Long timestamp;
}
