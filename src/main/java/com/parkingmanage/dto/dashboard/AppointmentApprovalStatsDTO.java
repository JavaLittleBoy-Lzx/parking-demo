package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 预约审批状态统计DTO
 */
@Data
public class AppointmentApprovalStatsDTO {
    /**
     * 审批状态
     */
    private String approvalStatus;
    
    /**
     * 数量
     */
    private Integer count;
} 