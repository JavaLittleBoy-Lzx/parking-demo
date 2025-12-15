package com.parkingmanage.dto.dashboard;

import lombok.Data;
import java.time.LocalDate;

/**
 * 车场预约统计DTO
 */
@Data
public class ParkAppointmentStatsDTO {
    /**
     * 车场名称
     */
    private String parkName;
    
    /**
     * 预约数量
     */
    private Integer appointmentCount;
    
    /**
     * 预约日期
     */
    private LocalDate appointmentDate;
} 