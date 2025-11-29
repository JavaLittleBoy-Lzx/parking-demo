package com.parkingmanage.dto.dashboard;

import lombok.Data;
import java.util.List;

/**
 * 车场车流量对比DTO
 */
@Data
public class ParkTrafficComparisonDTO {
    
    /**
     * 停车场名称列表
     */
    private List<String> parkNames;
    
    /**
     * 数据系列
     */
    private List<SeriesData> series;
    
    @Data
    public static class SeriesData {
        /**
         * 系列名称（入场/离场）
         */
        private String name;
        
        /**
         * 数据值
         */
        private List<Integer> data;
    }
} 