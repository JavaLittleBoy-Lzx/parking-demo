package com.parkingmanage.dto.dashboard;

import lombok.Data;

import java.util.List;

/**
 * 预约通过率趋势数据传输对象
 */
@Data
public class AppointmentApprovalTrendDTO {
    /**
     * 日期列表
     */
    private List<String> dates;
    
    /**
     * 通过率数据
     */
    private List<Double> approvalRates;
    
    /**
     * 申请总数数据
     */
    private List<Integer> totalApplications;
    
    /**
     * 通过数量数据
     */
    private List<Integer> approvedApplications;
} 