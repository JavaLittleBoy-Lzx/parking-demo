package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.ParkingTimeoutConfig;

/**
 * 停车超时配置Service接口
 * 
 * @author System
 */
public interface ParkingTimeoutConfigService extends IService<ParkingTimeoutConfig> {

    /**
     * 根据车场编码和车辆类型获取超时配置
     * 
     * @param yardCode 车场编码
     * @param vehicleType 车辆类型
     * @return 超时配置
     */
    ParkingTimeoutConfig getByYardCodeAndVehicleType(String yardCode, String vehicleType);
} 