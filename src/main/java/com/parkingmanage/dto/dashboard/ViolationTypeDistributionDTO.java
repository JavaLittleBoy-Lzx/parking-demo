package com.parkingmanage.dto.dashboard;

import lombok.Data;

/**
 * 违规类型分布数据传输对象
 */
@Data
public class ViolationTypeDistributionDTO {
    /**
     * 违规类型名称
     */
    private String name;
    
    /**
     * 违规次数
     */
    private Integer value;
} 