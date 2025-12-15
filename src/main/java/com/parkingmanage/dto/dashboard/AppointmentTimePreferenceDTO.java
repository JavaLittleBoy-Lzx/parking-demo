package com.parkingmanage.dto.dashboard;

import lombok.Data;
import java.util.List;

/**
 * 预约时段偏好DTO
 */
@Data
public class AppointmentTimePreferenceDTO {
    
    /**
     * 时段列表
     */
    private List<String> timePeriods;
    
    /**
     * 预约数量数据
     */
    private List<Integer> appointmentCounts;
    
    /**
     * 通过率数据
     */
    private List<Double> approvalRates;
} 