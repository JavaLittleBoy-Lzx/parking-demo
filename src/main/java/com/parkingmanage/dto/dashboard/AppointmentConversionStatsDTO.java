package com.parkingmanage.dto.dashboard;

import lombok.Data;
import java.time.LocalDate;

/**
 * 预约转化率统计DTO
 */
@Data
public class AppointmentConversionStatsDTO {
    /**
     * 预约日期
     */
    private LocalDate appointmentDate;
    
    /**
     * 总预约数
     */
    private Integer totalAppointments;
    
    /**
     * 实际进场数
     */
    private Integer actualEntries;
    
    /**
     * 转化率
     */
    private Double conversionRate;
} 