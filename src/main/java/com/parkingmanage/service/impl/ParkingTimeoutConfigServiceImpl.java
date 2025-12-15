package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.ParkingTimeoutConfig;
import com.parkingmanage.mapper.ParkingTimeoutConfigMapper;
import com.parkingmanage.service.ParkingTimeoutConfigService;
import org.springframework.stereotype.Service;

/**
 * 停车超时配置Service实现类
 * 
 * @author System
 */
@Service
public class ParkingTimeoutConfigServiceImpl extends ServiceImpl<ParkingTimeoutConfigMapper, ParkingTimeoutConfig> 
        implements ParkingTimeoutConfigService {

    @Override
    public ParkingTimeoutConfig getByYardCodeAndVehicleType(String yardCode, String vehicleType) {
        LambdaQueryWrapper<ParkingTimeoutConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ParkingTimeoutConfig::getYardCode, yardCode)
                   .eq(ParkingTimeoutConfig::getVehicleType, vehicleType)
                   .eq(ParkingTimeoutConfig::getIsActive, true);
        return this.getOne(queryWrapper);
    }
} 