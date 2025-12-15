package com.parkingmanage.dto.dashboard;

import lombok.Data;

import java.util.List;

/**
 * 车流量趋势数据传输对象
 */
@Data
public class TrafficTrendDTO {
    /**
     * 日期列表
     */
    private List<String> dates;
    
    /**
     * 图表数据系列
     */
    private List<SeriesData> series;
    
    @Data
    public static class SeriesData {
        /**
         * 系列名称
         */
        private String name;
        
        /**
         * 数据值列表
         */
        private List<Integer> data;
    }
} 