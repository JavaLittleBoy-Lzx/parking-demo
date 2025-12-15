package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.Rental;
import com.parkingmanage.vo.DeviceRentVo;

import javax.servlet.http.HttpServletResponse;

/**
 <p>
 设备租赁 服务类
 </p>

 @author yuli
 @since 2022-03-02
*/
public interface RentalService extends IService<Rental> {
    /**

     @param rental
    */
    void insertRental(Rental rental);

    /**

     @param deviceName
     @param customerName
     @param deviceCode
     @param response
    */
    void exportRental(String deviceName, String customerName, String deviceCode, HttpServletResponse response);

    /**
     归还
     @param rental
    */
    void updateTime(Rental rental);


    DeviceRentVo queryRental();
}
