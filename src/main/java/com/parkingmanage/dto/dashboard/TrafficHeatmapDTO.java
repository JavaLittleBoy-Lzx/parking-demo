package com.parkingmanage.dto.dashboard;

import lombok.Data;
import java.util.List;

/**
 * 车流热力图DTO
 */
@Data
public class TrafficHeatmapDTO {
    
    /**
     * 时间段列表（小时）
     */
    private List<String> hours;
    
    /**
     * 日期列表
     */
    private List<String> dates;
    
    /**
     * 热力图数据 [日期索引, 小时索引, 车流量]
     */
    private List<List<Object>> data;
    
    /**
     * 最大值（用于热力图颜色映射）
     */
    private Integer maxValue;
    
    /**
     * 最小值
     */
    private Integer minValue;
} 