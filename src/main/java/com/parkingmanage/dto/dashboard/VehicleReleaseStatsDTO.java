package com.parkingmanage.dto.dashboard;

import lombok.Data;
import java.time.LocalDate;

/**
 * 车辆放行统计DTO
 */
@Data
public class VehicleReleaseStatsDTO {
    /**
     * 车场名称
     */
    private String parkName;
    
    /**
     * 放行数量
     */
    private Integer releaseCount;
    
    /**
     * 放行日期
     */
    private LocalDate releaseDate;
} 