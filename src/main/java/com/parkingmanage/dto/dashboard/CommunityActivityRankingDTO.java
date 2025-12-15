package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 小区活跃度排行DTO
 */
@Data
public class CommunityActivityRankingDTO {
    /**
     * 小区名称
     */
    private String community;
    
    /**
     * 总预约数
     */
    private Integer totalAppointments;
    
    /**
     * 已批准数
     */
    private Integer approvedCount;
    
    /**
     * 实际到访数
     */
    private Integer actualVisitCount;
    
    /**
     * 近期预约数
     */
    private Integer recentAppointments;
    
    /**
     * 活跃度评分
     */
    private Double activityScore;
} 