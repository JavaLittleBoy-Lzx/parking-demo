package com.parkingmanage.mapper;

import com.parkingmanage.entity.TimeOutVehicleList;
import com.parkingmanage.entity.VehicleClassification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.VehicleReservation;

import java.util.ArrayList;
import java.util.List;

/**
 <p>
  Mapper 接口
 </p>

 @author 李子雄
 *
 */
public interface VehicleClassificationMapper extends BaseMapper<VehicleClassification> {

    int duplicate(VehicleClassification vehicleClassification);

    List<VehicleClassification> vehicleClassificationList();

    String getNameByCarNo(int enterCarType);

    ArrayList<TimeOutVehicleList> selectBytimeOutInterval(Integer timeOutInterval);

    ArrayList<TimeOutVehicleList> selectByEnterTimeOutInterval(Integer timeOutInterval,String yardName);

    ArrayList<TimeOutVehicleList> selectByMinutesEnterTimeOutInterval(Integer timeOutInterval);
}
