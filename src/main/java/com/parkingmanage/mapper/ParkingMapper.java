package com.parkingmanage.mapper;

import com.parkingmanage.entity.Parking;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.Patrol;

import java.util.List;

/**
 * <p>
 *  Mapper 接口 
 * </p>
 *
 * @author MLH
 * @since 2022-11-05
 */
public interface ParkingMapper extends BaseMapper<Parking> {
    List<Parking> getList(String province, String city, String district, String community);
}