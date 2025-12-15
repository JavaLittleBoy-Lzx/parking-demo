package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 月票状态分布DTO
 */
@Data
public class MonthTicketStatusDistributionDTO {
    
    /**
     * 状态名称
     */
    private String statusName;
    
    /**
     * 数量
     */
    private Integer count;
    
    /**
     * 百分比
     */
    private Double percentage;
} 