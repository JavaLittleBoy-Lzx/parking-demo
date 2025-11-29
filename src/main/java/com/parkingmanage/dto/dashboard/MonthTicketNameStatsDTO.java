package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 月票名称统计DTO
 */
@Data
public class MonthTicketNameStatsDTO {
    /**
     * 月票名称
     */
    private String ticketName;
    
    /**
     * 总数量
     */
    private Integer count;
    
    /**
     * 有效数量
     */
    private Integer activeCount;
} 