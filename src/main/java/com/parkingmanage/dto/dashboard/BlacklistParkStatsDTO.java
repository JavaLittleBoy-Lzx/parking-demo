package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 黑名单车场统计DTO
 */
@Data
public class BlacklistParkStatsDTO {
    /**
     * 车场名称
     */
    private String parkName;
    
    /**
     * 数量
     */
    private Integer count;
} 