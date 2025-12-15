package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 巡逻员小区统计DTO
 */
@Data
public class PatrolCommunityStatsDTO {
    /**
     * 小区名称
     */
    private String communityName;
    
    /**
     * 巡逻员数量
     */
    private Integer patrolCount;
} 