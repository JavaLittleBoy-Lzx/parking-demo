package com.parkingmanage.service.impl;

import com.parkingmanage.entity.Parking;
import com.parkingmanage.mapper.ParkingMapper;
import com.parkingmanage.service.ParkingService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 <p>
  服务实现类
 </p>

 @author MLH
 @since 2022-11-05
*/
@Service
public class ParkingServiceImpl extends ServiceImpl<ParkingMapper, Parking> implements ParkingService {
    @Override
    public  List<Parking> getList(String province, String city, String district, String community){
        return baseMapper.getList(province,city,district,community);
    }
}
