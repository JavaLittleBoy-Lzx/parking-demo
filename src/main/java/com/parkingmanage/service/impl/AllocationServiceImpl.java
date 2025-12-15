package com.parkingmanage.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.Allocation;
import com.parkingmanage.entity.Department;
import com.parkingmanage.entity.Device;
import com.parkingmanage.entity.User;
import com.parkingmanage.mapper.AllocationMapper;
import com.parkingmanage.mapper.DepartmentMapper;
import com.parkingmanage.mapper.DeviceMapper;
import com.parkingmanage.service.AllocationService;
import com.parkingmanage.service.DeviceService;
import com.parkingmanage.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 调拨管理 服务实现类
 * </p>
 *
 * @author yuli
 * @since 2022-03-02
 */
@Service
public class AllocationServiceImpl extends ServiceImpl<AllocationMapper, Allocation> implements AllocationService {
    @Resource
    private DeviceMapper deviceMapper;
    @Resource
    private DepartmentMapper departmentMapper;
    @Resource
    private DeviceService deviceService;
    @Resource
    private UserService userService;

    @Override
    public void saveAllocation(Allocation allocation) {
        if (allocation != null && allocation.getDeviceId() != null) {
            Device device = deviceMapper.selectById(allocation.getDeviceId());
            if (!ObjectUtils.isEmpty(device)) {
                allocation.setDeviceId(device.getDeviceId());
                allocation.setDeviceName(device.getDeviceName());
                allocation.setDeviceCode(device.getDeviceCode());
                allocation.setDepartmentId(device.getDepartmentId());//原部门
                allocation.setApplicationTime(LocalDateTime.now());
                device.setOriginalState(device.getDeviceStatus());
                device.setDeviceStatus(5);
                deviceService.updateById(device);
            }
            this.save(allocation);
        }
    }

    @Override
    public List<Allocation> queryList(String name, String deviceCode) {
        List<Allocation> allocations = this.list(Wrappers.<Allocation>lambdaQuery()
                .like(StringUtils.hasLength(name), Allocation::getDeviceName, name)
                .like(StringUtils.hasLength(deviceCode), Allocation::getDeviceCode, deviceCode));
        for (Allocation allocation : allocations) {
            Department department = departmentMapper.selectById(allocation.getDepartmentId());
            if (department != null) {
                allocation.setDepartmentName(department.getDepartmentName());
            }
            Department newdepartment = departmentMapper.selectById(allocation.getAfterDepartmentId());
            if (newdepartment != null) {
                allocation.setAfterDepartmentName(newdepartment.getDepartmentName());
            }
            User user = userService.getById(allocation.getAuditUserId());
            if (user != null) {
                allocation.setAuditUserName(user.getUserName());
            }
            User newuser = userService.getById(allocation.getApplicantUserId());
            if (newuser != null) {
                allocation.setApplicantUserName(newuser.getUserName());
            }

        }
        return allocations;
    }

    @Override
    public void exportAllocation(String deviceName, String deviceCode, HttpServletResponse response) {
        List<Allocation> allocationList = queryList(deviceName, deviceCode);
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("deviceName", "设备名称");
        writer.addHeaderAlias("departmentName", "原部门");
        writer.addHeaderAlias("afterDepartmentName", "调往部门");
        writer.addHeaderAlias("deviceCode", "设备编码");
        writer.addHeaderAlias("applicantUserName", "申请人");
        writer.addHeaderAlias("applicationReason", "申请原因");
        writer.addHeaderAlias("applicationTime", "申请时间");
        writer.addHeaderAlias("auditUserName", "审批人");
        writer.addHeaderAlias("remarks", "备注");
        writer.setOnlyAlias(true);
        writer.write(allocationList, true);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=allocationListexcel.xlsx");
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
