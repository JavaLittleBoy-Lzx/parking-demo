package com.parkingmanage.dto.dashboard;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 重复放行车辆统计DTO
 */
@Data
public class RepeatReleaseStatsDTO {
    /**
     * 车牌号
     */
    private String plateNumber;
    
    /**
     * 放行次数
     */
    private Integer releaseCount;
    
    /**
     * 涉及车场
     */
    private String parkNames;
    
    /**
     * 最后放行时间
     */
    private LocalDateTime lastReleaseTime;
} 