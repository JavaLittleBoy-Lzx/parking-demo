package com.parkingmanage.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.smallbun.screw.core.util.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.common.exception.CustomException;
import com.parkingmanage.entity.Department;
import com.parkingmanage.entity.Device;
import com.parkingmanage.entity.Purchase;
import com.parkingmanage.mapper.DepartmentMapper;
import com.parkingmanage.mapper.DeviceMapper;
import com.parkingmanage.service.DeviceService;
import com.parkingmanage.service.PurchaseService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 <p>
 设备信息 服务实现类
 </p>

 @author yuli
 @since 2022-03-01
*/
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {
    @Resource
    private PurchaseService purchaseService;
    @Resource
    private DepartmentMapper departmentMapper;

    @Override
    public void insertDevice(Device device) {
        if (device.getPurchaseId() != null) {
//设备编码 时间
            Purchase purchase = purchaseService.getById(device.getPurchaseId());
            List<Device> devices = this.list(new LambdaQueryWrapper<Device>().
                    eq(Device::getPurchaseId, device.getPurchaseId()));
            if (!CollectionUtils.isEmpty(devices) && devices.size() >= purchase.getOrderQuantity()) {
                throw new CustomException("99", "数量超过预购数量");
            }
            //long System.currentTimeMillis(
            device.setDeviceCode(String.valueOf(System.currentTimeMillis()));
            device.setDeviceName(purchase.getDeviceName());
            device.setDevicePrice(purchase.getDevicePrice());
            device.setDepartmentId(purchase.getDepartmentId());
            device.setPurchaseTime(LocalDateTime.now());
            this.save(device);
        }
    }

    @Override
    public List<Device> queryList(String deviceName, String deviceCode, String deviceType, Integer departmentId) {
        List<Device> devices = this.list(new LambdaQueryWrapper<Device>().
                like(StringUtils.hasLength(deviceName), Device::getDeviceName, deviceName)
                .like(StringUtils.hasLength(deviceCode), Device::getDeviceCode, deviceType)
                .like(StringUtils.hasLength(deviceType), Device::getDeviceType, deviceType)
                .eq(departmentId != null, Device::getDepartmentId, departmentId));
        for (Device device : devices) {
            if (1 == device.getDeviceType()) {
                device.setDeviceTypeName("生产设备");
            }
            if (2 == device.getDeviceType()) {
                device.setDeviceTypeName("电气设备");
            }
            if (3 == device.getDeviceType()) {
                device.setDeviceTypeName("特种设备");
            }
            if (4 == device.getDeviceType()) {
                device.setDeviceTypeName("精密设备");
            }
            if (5 == device.getDeviceType()) {
                device.setDeviceTypeName("动力设备");
            }
            if (6 == device.getDeviceType()) {
                device.setDeviceTypeName("压力设备");
            }
            if (1 == device.getDeviceStatus()) {
                device.setDeviceStatusName("1空闲");
            }
            if (2 == device.getDeviceStatus()) {
                device.setDeviceStatusName("2使用中");
            }
            if (3 == device.getDeviceStatus()) {
                device.setDeviceStatusName("3维修中");
            }
            if (4 == device.getDeviceStatus()) {
                device.setDeviceStatusName("4租赁");
            }
            if (5 == device.getDeviceStatus()) {
                device.setDeviceStatusName("5调拨");
            }
            if (6 == device.getDeviceStatus()) {
                device.setDeviceStatusName("6报废");
            }
            Department department = departmentMapper.selectById(device.getDepartmentId());
            if (department != null) {
                device.setDepartmentName(department.getDepartmentName());
            }

        }
        return devices;
    }

    @Override
    public void exportDevice(HttpServletResponse response) {
        List<Device> devices = this.queryList(null, null, null, null);
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("deviceName", "设备名称");
        writer.addHeaderAlias("devicePrice", "设备价格");
        writer.addHeaderAlias("deviceCode", "设备编码");
        writer.addHeaderAlias("model", "规格型号");
        writer.addHeaderAlias("deviceTypeName", "设备类型");
        writer.addHeaderAlias("departmentName", "部门名称");
        writer.addHeaderAlias("deviceStatusName", "设备状态");
        writer.setOnlyAlias(true);
        writer.write(devices, true);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=deviceInfoasexcel.xlsx");
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
