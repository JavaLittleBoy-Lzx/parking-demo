package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.Maintenance;
import com.parkingmanage.vo.DeviceRentVo;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 <p>
 维修管理 服务类
 </p>

 @author yuli
 @since 2022-03-01
*/
public interface MaintenanceService extends IService<Maintenance> {

    void insertMaintenance(Maintenance maintenance);
    /**

    */
    List<Maintenance> queryMaintenanceList(String deviceName,String departmentIdm,String deviceCode);

    /**

     @param response
    */
    void exportMaintenance(HttpServletResponse response);

    DeviceRentVo queryMaintenance();
}

