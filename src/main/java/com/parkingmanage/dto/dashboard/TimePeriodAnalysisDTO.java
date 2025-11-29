package com.parkingmanage.dto.dashboard;

import lombok.Data;

import java.util.List;

/**
 * 时段分析数据传输对象
 */
@Data
public class TimePeriodAnalysisDTO {
    /**
     * 时段标签列表
     */
    private List<String> periods;
    
    /**
     * 分析数据系列
     */
    private List<AnalysisData> series;
    
    @Data
    public static class AnalysisData {
        /**
         * 数据类型名称
         */
        private String name;
        
        /**
         * 数据值列表
         */
        private List<Double> data;
    }
} 