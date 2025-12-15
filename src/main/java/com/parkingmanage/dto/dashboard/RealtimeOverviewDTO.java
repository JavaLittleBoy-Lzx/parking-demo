package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 实时统计概览数据传输对象
 */
@Data
public class RealtimeOverviewDTO {
    /**
     * 停车场利用率
     */
    private String parkingUtilization;
    
    /**
     * 月票数量
     */
    private String monthTicketCount;
    
    /**
     * 预约通过率
     */
    private String approvalRate;
    
    /**
     * 待处理违规数
     */
    private String pendingViolations;
    
    /**
     * 今日收入
     */
    private String todayRevenue;
    
    /**
     * 设备在线率
     */
    private String equipmentOnlineRate;
    
    /**
     * 黑名单数量
     */
    private String blacklistCount;
    
    /**
     * 系统活跃度
     */
    private String systemActivity;
} 