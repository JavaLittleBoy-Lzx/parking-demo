package com.parkingmanage.dto.dashboard;

import lombok.Data;
import java.util.List;

/**
 * 车场利用率排行DTO
 */
@Data
public class ParkUtilizationRankingDTO {
    
    /**
     * 排行数据列表
     */
    private List<RankingItem> rankings;
    
    @Data
    public static class RankingItem {
        /**
         * 停车场名称
         */
        private String parkName;
        
        /**
         * 停车场编码
         */
        private String parkCode;
        
        /**
         * 利用率（百分比）
         */
        private Double utilizationRate;
        
        /**
         * 排名
         */
        private Integer rank;
        
        /**
         * 总车位数
         */
        private Integer totalSpaces;
        
        /**
         * 已用车位数
         */
        private Integer usedSpaces;
    }
} 