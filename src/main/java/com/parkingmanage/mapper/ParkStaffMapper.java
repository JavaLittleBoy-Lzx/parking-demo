package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.ParkStaff;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 车场人员Mapper接口
 * 
 * @author parking-system
 * @version 1.0
 */
@Mapper
public interface ParkStaffMapper extends BaseMapper<ParkStaff> {
    
    /**
     * 巡检人员状态统计
     */
    List<Map<String, Object>> selectStatusStats();
    
    /**
     * 巡检员发现问题类型分布
     */
    List<Map<String, Object>> selectProblemTypeDistribution(@Param("days") Integer days);
    
} 