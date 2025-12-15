package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 管家小区统计DTO
 */
@Data
public class ButlerCommunityStatsDTO {
    /**
     * 小区名称
     */
    private String communityName;
    
    /**
     * 管家数量
     */
    private Integer butlerCount;
} 