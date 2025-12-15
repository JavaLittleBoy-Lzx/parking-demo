package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.ParkStaff;
import com.parkingmanage.mapper.ParkStaffMapper;
import com.parkingmanage.service.ParkStaffService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 车场人员服务实现类
 * 
 * @author parking-system
 * @version 1.0
 */
@Slf4j
@Service
public class ParkStaffServiceImpl extends ServiceImpl<ParkStaffMapper, ParkStaff> implements ParkStaffService {
    
    @Resource
    private ParkStaffMapper parkStaffMapper;
    
    // ==================== 📊 统计分析实现 ====================
    
    @Override
    public List<Map<String, Object>> getStatusStats() {
        log.info("📊 查询巡检人员状态统计");
        return parkStaffMapper.selectStatusStats();
    }
    
    @Override
    public List<Map<String, Object>> getProblemTypeDistribution(Integer days) {
        log.info("📊 查询巡检员发现问题类型分布, 近{}天", days);
        return parkStaffMapper.selectProblemTypeDistribution(days);
    }
    
} 