package com.parkingmanage.service;

import com.parkingmanage.entity.Allocation;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 <p>
 调拨管理 服务类
 </p>

 @author yuli
 @since 2022-03-02updateById
*/
public interface AllocationService extends IService<Allocation> {

    void saveAllocation(Allocation allocation);

    List<Allocation> queryList(String name, String deviceCode);

    void exportAllocation(String deviceName, String deviceCode, HttpServletResponse response);
}
