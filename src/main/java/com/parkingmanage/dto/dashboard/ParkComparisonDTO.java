package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 各车场对比数据传输对象
 */
@Data
public class ParkComparisonDTO {
    /**
     * 停车场名称
     */
    private String parkName;
    
    /**
     * 停车场代码
     */
    private String parkCode;
    
    /**
     * 对比数值
     */
    private Double value;
    
    /**
     * 单位
     */
    private String unit;
    
    /**
     * 相比昨日变化率
     */
    private Double changeRate;
} 