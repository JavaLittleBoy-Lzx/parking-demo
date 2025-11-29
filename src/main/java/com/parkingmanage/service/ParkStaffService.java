package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.ParkStaff;

import java.util.List;
import java.util.Map;

/**
 * 车场人员服务接口
 * 
 * @author parking-system
 * @version 1.0
 */
public interface ParkStaffService extends IService<ParkStaff> {
    
    // ==================== 📊 统计分析接口 ====================
    
    /**
     * 巡检人员状态统计
     * @return 统计结果
     */
    List<Map<String, Object>> getStatusStats();
    
    /**
     * 巡检员发现问题类型分布
     * @param days 统计天数
     * @return 统计结果
     */
    List<Map<String, Object>> getProblemTypeDistribution(Integer days);
    
} 