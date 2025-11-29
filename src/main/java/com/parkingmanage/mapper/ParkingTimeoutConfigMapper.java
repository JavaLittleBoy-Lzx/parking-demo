package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.ParkingTimeoutConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 停车超时配置Mapper接口
 * 
 * @author System
 */
@Mapper
public interface ParkingTimeoutConfigMapper extends BaseMapper<ParkingTimeoutConfig> {

    /**
     * 根据车场编码和车辆类型获取超时配置
     * 
     * @param yardCode 车场编码
     * @param vehicleType 车辆类型
     * @return 超时配置
     */
    ParkingTimeoutConfig getByYardCodeAndVehicleType(@Param("yardCode") String yardCode, 
                                                   @Param("vehicleType") String vehicleType);
} 