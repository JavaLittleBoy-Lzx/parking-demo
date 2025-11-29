package com.parkingmanage.service;

import com.parkingmanage.entity.Parking;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 <p>
  服务类
 </p>

 @author MLH
 @since 2022-11-05
*/
public interface ParkingService extends IService<Parking> {
    List<Parking> getList(String province,String city,String district,String community);
}
