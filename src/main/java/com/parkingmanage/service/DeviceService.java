package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.Device;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 <p>
 设备信息 服务类
 </p>

 @author yuli
 @since 2022-03-01
*/
public interface DeviceService extends IService<Device> {
    /**
     田间
     @param device
    */
    void insertDevice(Device device);

    /**
     根据
     @param deviceName
     @param deviceCode
     @param deviceType
     @return
    */
    List<Device> queryList(String deviceName,String deviceCode,String deviceType,Integer departmentId );

    /**
     文excel 件
     @param response
    */
    void exportDevice(HttpServletResponse response);
}
