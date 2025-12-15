package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 月票车场统计DTO
 */
@Data
public class MonthTicketParkStatsDTO {
    /**
     * 车场名称
     */
    private String parkName;
    
    /**
     * 总数量
     */
    private Integer count;
    
    /**
     * 有效数量
     */
    private Integer activeCount;
} 