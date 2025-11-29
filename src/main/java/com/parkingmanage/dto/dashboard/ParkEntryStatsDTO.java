package com.parkingmanage.dto.dashboard;

import lombok.Data;
import java.time.LocalDate;

/**
 * 车场进场统计DTO
 */
@Data
public class ParkEntryStatsDTO {
    /**
     * 车场名称
     */
    private String parkName;
    
    /**
     * 进场数量
     */
    private Integer entryCount;
    
    /**
     * 进场日期
     */
    private LocalDate entryDate;
} 