package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 黑名单违规类型统计DTO
 */
@Data
public class BlacklistViolationStatsDTO {
    /**
     * 违规类型
     */
    private String violationType;
    
    /**
     * 数量
     */
    private Integer count;
} 