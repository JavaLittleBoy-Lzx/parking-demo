package com.parkingmanage.dto.dashboard;

import lombok.Data;

import java.util.List;

/**
 * 月票使用率分析数据传输对象
 */
@Data
public class MonthTicketUsageDTO {
    /**
     * 总月票数量
     */
    private Integer totalTickets;
    
    /**
     * 已使用月票数量
     */
    private Integer usedTickets;
    
    /**
     * 使用率百分比
     */
    private Double usageRate;
    
    /**
     * 各停车场使用情况
     */
    private List<ParkUsage> parkUsages;
    
    @Data
    public static class ParkUsage {
        /**
         * 停车场名称
         */
        private String parkName;
        
        /**
         * 停车场代码
         */
        private String parkCode;
        
        /**
         * 该停车场月票数量
         */
        private Integer ticketCount;
        
        /**
         * 该停车场使用率
         */
        private Double usageRate;
    }
} 