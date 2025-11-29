package com.parkingmanage.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.smallbun.screw.core.util.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.Department;
import com.parkingmanage.entity.Device;
import com.parkingmanage.entity.Maintenance;
import com.parkingmanage.entity.User;
import com.parkingmanage.mapper.DepartmentMapper;
import com.parkingmanage.mapper.DeviceMapper;
import com.parkingmanage.mapper.MaintenanceMapper;
import com.parkingmanage.service.DeviceService;
import com.parkingmanage.service.MaintenanceService;
import com.parkingmanage.service.UserService;
import com.parkingmanage.vo.DeviceRentVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 <p>
 维修管理 服务实现类
 </p>

 @author yuli
 @since 2022-03-01
*/
@Service
public class MaintenanceServiceImpl extends ServiceImpl<MaintenanceMapper, Maintenance> implements MaintenanceService {
    @Resource
    DeviceMapper deviceMapper;
    @Resource
    DepartmentMapper departmentMapper;
    @Resource
    DeviceService deviceService;
    @Resource
    UserService userService;
    @Resource
    MaintenanceMapper maintenanceMapper;

    @Transactional
    @Override
    public void insertMaintenance(Maintenance maintenance) {
        if (maintenance.getDeviceId() != null) {
            Device device = deviceMapper.selectById(maintenance.getDeviceId());
            if (!ObjectUtils.isEmpty(device)) {
                maintenance.setDeviceId(device.getDeviceId());
                maintenance.setDeviceName(device.getDeviceName());
                maintenance.setDeviceCode(device.getDeviceCode());
                maintenance.setDeviceType(device.getDeviceType());
                device.setOriginalState(device.getDeviceStatus());
                device.setDeviceStatus(3);
                deviceService.updateById(device);
            }
            Department department = departmentMapper.selectById(device.getDepartmentId());
            if (!ObjectUtils.isEmpty(department)) {
                maintenance.setDepartmentAddress(department.getDepartmentAddress());
                maintenance.setDepartmentId(department.getDepartmentId());
            }

            maintenance.setRepairTime(LocalDateTime.now());
            this.save(maintenance);
        }
    }

    @Override
    public List<Maintenance> queryMaintenanceList(String deviceName, String departmentId, String deviceCode) {
        List<Maintenance> maintenances = this.list(Wrappers.<Maintenance>lambdaQuery()
                .like(StringUtils.hasLength(deviceName), Maintenance::getDeviceName, deviceName)
                .likeRight(StringUtils.hasLength(deviceCode), Maintenance::getDeviceCode, deviceCode)
                .eq(StringUtils.hasLength(departmentId), Maintenance::getDepartmentId, departmentId));
        for (Maintenance maintenance : maintenances) {
            User user = userService.getById(maintenance.getMaintenanceUserId());
            if (user != null) {
                maintenance.setMaintenanceUserName(user.getUserName());
            }
            User on = userService.getById(maintenance.getRepairmanUserId());
            if (on != null) {
                maintenance.setRepairmanUserName(on.getUserName());
            }
        }
        return maintenances;
    }

    @Override
    public void exportMaintenance(HttpServletResponse response) {
        List<Maintenance> maintenances = this.queryMaintenanceList(null, null, null);

        if (!CollectionUtils.isEmpty(maintenances)) {
            for (Maintenance maintenance : maintenances) {
                if (maintenance.getAuditStatus() == 1) {
                    maintenance.setAuditStatusName("待维修");
                } else if (maintenance.getAuditStatus() == 2) {
                    maintenance.setAuditStatusName("已维修");
                }
            }
            ExcelWriter writer = ExcelUtil.getWriter(true);
            writer.addHeaderAlias("deviceName", "设备名称");
            writer.addHeaderAlias("faultDescription", "故障描述");
            writer.addHeaderAlias("departmentAddress", "部门地址");
            writer.addHeaderAlias("deviceCode", "设备编码");
            writer.addHeaderAlias("repairmanUserName", "维修人");
            writer.addHeaderAlias("maintenanceUserName", "申请人");
            writer.addHeaderAlias("auditStatusName", "设备状态");
            writer.addHeaderAlias("remarks", "备注");
            writer.setOnlyAlias(true);
            writer.write(maintenances, true);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=maintenanceoasexcel.xlsx");
            ServletOutputStream out = null;
            try {
                out = response.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            writer.flush(out, true);
            writer.close();
            IoUtil.close(out);
        }
    }

    @Override
    public DeviceRentVo queryMaintenance() {
        DeviceRentVo deviceRentVo = new DeviceRentVo();
        List<Maintenance> maintenances = maintenanceMapper.getByTypeMaintenance();
        if (!CollectionUtils.isEmpty(maintenances)) {
            Map<Integer, List<Maintenance>> deviceType = maintenances.stream().collect(Collectors.groupingBy(Maintenance::getDeviceType));
            List<String> typeName = new ArrayList<>();
            List<Integer> rentalNum = new ArrayList<>();
            deviceType.forEach((deviceTypeId, value) -> {
                if (deviceTypeId == 1) {
                    typeName.add("生产设备");
                } else if (deviceTypeId == 2) {
                    typeName.add("电气设备");
                } else if (deviceTypeId == 3) {
                    typeName.add("特种设备");
                } else if (deviceTypeId == 4) {
                    typeName.add("精密设备");
                } else if (deviceTypeId == 5) {
                    typeName.add("动力设备");
                } else if (deviceTypeId == 6) {
                    typeName.add("压力设备");
                }
                rentalNum.add(value.size());

            });
            deviceRentVo.setTypeName(typeName);
            deviceRentVo.setRentalNum(rentalNum);
        }
        return deviceRentVo;
    }

}

