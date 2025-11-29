package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 业主小区统计DTO
 */
@Data
public class OwnerCommunityStatsDTO {
    /**
     * 小区名称
     */
    private String communityName;
    
    /**
     * 业主数量
     */
    private Integer ownerCount;
    
    /**
     * 车辆数量
     */
    private Integer vehicleCount;
} 