package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 车辆类型统计DTO
 */
@Data
public class VehicleTypeStatsDTO {
    /**
     * 车辆类型 (小型车/大型车/新能源)
     */
    private String vehicleType;
    
    /**
     * 数量
     */
    private Integer count;
    
    /**
     * 占比
     */
    private Double percentage;
} 