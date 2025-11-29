package com.parkingmanage.dto.ai.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 数据报告生成请求
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataReportRequest {

    /**
     * 报告类型
     */
    private String reportType;

    /**
     * 统计数据
     */
    private Object statistics;
}