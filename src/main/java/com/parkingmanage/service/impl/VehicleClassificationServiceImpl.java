package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkingmanage.entity.TimeOutVehicleList;
import com.parkingmanage.entity.VehicleClassification;
import com.parkingmanage.entity.VehicleReservation;
import com.parkingmanage.entity.YardInfo;
import com.parkingmanage.mapper.VehicleClassificationMapper;
import com.parkingmanage.service.VehicleClassificationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 <p>
  服务实现类
 </p>

 @author 李子雄

*/
@Service
public class VehicleClassificationServiceImpl extends ServiceImpl<VehicleClassificationMapper, VehicleClassification> implements VehicleClassificationService {

    @Resource
    private VehicleClassificationService vehicleClassificationService;
    @Override
    public int duplicate(VehicleClassification vehicleClassification) {
        return baseMapper.duplicate(vehicleClassification);
    }

    @Override
    public List<VehicleClassification> vehicleClassificationList() {
        return baseMapper.vehicleClassificationList();
    }

    @Override
    public List<VehicleClassification> queryListVehicleClassification(String vehicleClassification, String classificationNo) {
        LambdaQueryWrapper<VehicleClassification> queryWrapper = new LambdaQueryWrapper();

        if (StringUtils.hasLength(vehicleClassification)) {
            queryWrapper.like(VehicleClassification::getVehicleClassification, vehicleClassification);
        }
        if (StringUtils.hasLength(classificationNo)) {
            queryWrapper.like(VehicleClassification::getClassificationNo, classificationNo);
        }
        List<VehicleClassification> vehicleClassificationList = vehicleClassificationService.list(queryWrapper);
        return vehicleClassificationList;
    }

    @Override
    public String getNameByCarNo(int enterCarType) {
        return baseMapper.getNameByCarNo(enterCarType);
    }

    @Override
    public ArrayList<TimeOutVehicleList> selectBytimeOutInterval(Integer timeOutInterval) {
        return baseMapper.selectBytimeOutInterval(timeOutInterval);
    }

    @Override
    public ArrayList<TimeOutVehicleList> selectByEnterTimeOutInterval(Integer timeOutInterval,String yardName) {
        return baseMapper.selectByEnterTimeOutInterval(timeOutInterval,yardName);
    }

    @Override
    public ArrayList<TimeOutVehicleList> selectByMinutesEnterTimeOutInterval(Integer timeOutInterval) {
        return baseMapper.selectByMinutesEnterTimeOutInterval(timeOutInterval);
    }
}
