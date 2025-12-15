package com.parkingmanage.mapper;

import com.parkingmanage.entity.Maintenance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 <p>
 维修管理 Mapper 接口
 </p>

 @author yuli
 @since 2022-03-01
*/
public interface MaintenanceMapper extends BaseMapper<Maintenance> {

    List<Maintenance> getByTypeMaintenance();
}
