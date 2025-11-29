package com.parkingmanage.service;

import com.parkingmanage.entity.TimeOutVehicleList;
import com.parkingmanage.entity.VehicleClassification;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.VehicleReservation;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 李子雄
 *
 */
public interface VehicleClassificationService extends IService<VehicleClassification> {

    int duplicate(VehicleClassification vehicleClassification);

    List<VehicleClassification> vehicleClassificationList();

    List<VehicleClassification> queryListVehicleClassification(String vehicleClassification, String classificationNo);

    String getNameByCarNo(int enterCarType);

    ArrayList<TimeOutVehicleList> selectBytimeOutInterval(Integer timeOutInterval);

    ArrayList<TimeOutVehicleList> selectByEnterTimeOutInterval(Integer timeOutInterval,String yardName);

    ArrayList<TimeOutVehicleList> selectByMinutesEnterTimeOutInterval(Integer timeOutInterval);

}
