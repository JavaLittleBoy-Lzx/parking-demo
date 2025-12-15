package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 违规处理统计DTO
 */
@Data
public class ViolationHandlingStatsDTO {
    /**
     * 处理状态
     */
    private String handleStatus;
    
    /**
     * 数量
     */
    private Integer count;
} 