package com.parkingmanage.dto.dashboard;

import lombok.Data;
import java.util.List;

/**
 * 工作日vs周末对比DTO
 */
@Data
public class WeekdayWeekendComparisonDTO {
    
    /**
     * 时段列表
     */
    private List<String> timePeriods;
    
    /**
     * 数据系列
     */
    private List<SeriesData> series;
    
    @Data
    public static class SeriesData {
        /**
         * 系列名称（工作日入场、工作日离场、周末入场、周末离场）
         */
        private String name;
        
        /**
         * 数据值
         */
        private List<Integer> data;
        
        /**
         * 数据类型（weekday/weekend）
         */
        private String type;
    }
} 