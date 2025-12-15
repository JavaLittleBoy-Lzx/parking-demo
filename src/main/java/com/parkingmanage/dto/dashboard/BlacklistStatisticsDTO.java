package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 黑名单统计DTO
 */
@Data
public class BlacklistStatisticsDTO {
    
    /**
     * 黑名单原因
     */
    private String reason;
    
    /**
     * 数量
     */
    private Integer count;
    
    /**
     * 百分比
     */
    private Double percentage;
} 