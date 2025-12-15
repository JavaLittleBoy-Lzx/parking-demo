package com.parkingmanage.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.common.exception.CustomException;
import com.parkingmanage.entity.*;
import com.parkingmanage.mapper.ScrapMapper;
import com.parkingmanage.mapper.UserMapper;
import com.parkingmanage.service.DepartmentService;
import com.parkingmanage.service.DeviceService;
import com.parkingmanage.service.MaintenanceService;
import com.parkingmanage.service.ScrapService;
import com.parkingmanage.vo.ScrapMaintenceDto;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 <p>
 报废管理 服务实现类
 </p>

 @author yuli
 @since 2022-03-04
*/
@Service
public class ScrapServiceImpl extends ServiceImpl<ScrapMapper, Scrap> implements ScrapService {

    @Resource
    private DeviceService deviceService;
    @Resource
    private ScrapService scrapService;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private UserMapper userMapper;
    @Resource
    private MaintenanceService maintenanceService;


    @Override
    @Transactional
    public void saveScrap(Scrap scrap) {
        if (scrap.getDeviceId() != null) {
            Device device = deviceService.getById(scrap.getDeviceId());
            if (ObjectUtils.isNotEmpty(device)) { //
                scrap.setDepartmentId(device.getDepartmentId());
                scrap.setDeviceName(device.getDeviceName());
                scrap.setDeviceCode(device.getDeviceCode());
                device.setOriginalState(device.getDeviceStatus());
                device.setDeviceStatus(9);
                deviceService.updateById(device);
            }
            scrap.setScrapDate(LocalDateTime.now());
            scrapService.save(scrap);
        } else {
            throw new CustomException("99", "设备id必须填写");
        }
    }

    @Override
    public List<Scrap> queryListScrap(String deviceName, String scrapDate, String deviceCode) {

        LambdaQueryWrapper<Scrap> queryWrapper = new LambdaQueryWrapper();
        if (StringUtils.hasLength(deviceName)) {
            queryWrapper.like(Scrap::getDeviceName, deviceName);
        }
        if (StringUtils.hasLength(scrapDate)) {
            queryWrapper.like(Scrap::getScrapDate, scrapDate);
        }
        if (StringUtils.hasLength(deviceCode)) {
            queryWrapper.like(Scrap::getDeviceCode, deviceCode);
        }
        List<Scrap> scraps = scrapService.list(queryWrapper);
        if (!CollectionUtils.isEmpty(scraps)) {
            for (Scrap scrap : scraps) {
                Department department = departmentService.getById(scrap.getDepartmentId());
                if (department != null) {
                    scrap.setDepartmentName(department.getDepartmentName());
                }
                User user = userMapper.selectById(scrap.getAudiusUserId());
                if (ObjectUtils.isNotEmpty(user)) {
                    scrap.setAudiusUserName(user.getUserName());
                }
                User use = userMapper.selectById(scrap.getRegistrationUserId());
                if (ObjectUtils.isNotEmpty(use)) {
                    scrap.setRegistrationUserName(use.getUserName());
                }
            }
        }
        return scraps;
    }

    @Override
    public void updateScrapAndDevice(Scrap scrap) {
        if (scrap.getDeviceId() != null && scrap.getDeviceStatus() != null && scrap.getScrapId() != null) {
            Device device = deviceService.getById(scrap.getDeviceId());
            if (ObjectUtils.isNotEmpty(device)) {
                device.setDeviceStatus(6);
                deviceService.updateById(device);
            }
        }
        scrap.setApprovalTime(LocalDateTime.now());
        this.updateById(scrap);
    }

    @Override
    public void exportScrap(String deviceName, String deviceCode, String scrapDate, HttpServletResponse response) {
        List<Scrap> scrapList = scrapService.queryListScrap(deviceName, scrapDate, deviceCode);
        //按照设备名和申请日期排序
        List<Scrap> scraps = scrapList.stream().sorted(Comparator.comparing(Scrap::getDeviceName).thenComparing(Scrap::getScrapDate)).collect(Collectors.toList());
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("deviceName", "设备名称");
        writer.addHeaderAlias("deviceCode", "设备编码");
        writer.addHeaderAlias("departmentName", "使用部门");
        writer.addHeaderAlias("scrapReason", "报废原因");
        writer.addHeaderAlias("registrationUserName", "申请人");
        writer.addHeaderAlias("scrapDate", "报废日期");
        writer.addHeaderAlias("audiusUserName", "审批人");
        writer.addHeaderAlias("approvalTime", "实际报废审批时间");
        writer.addHeaderAlias("remarks", "备注");
        writer.setOnlyAlias(true);
        writer.write(scraps, true);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=scrapListexcel.xlsx");
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

    @Override
    @Transactional
    public void auditScrapAndMaintence(ScrapMaintenceDto scrapMaintenceDto) {
        if (scrapMaintenceDto.getScrapId() != null) {
            Scrap scrap = scrapService.getById(scrapMaintenceDto.getScrapId());
            if (!ObjectUtils.isEmpty(scrap)) {
                scrap.setAudiusReason(scrapMaintenceDto.getAudiusReason());
                scrap.setDeviceStatus(scrapMaintenceDto.getDeviceStatus() != null ? scrapMaintenceDto.getDeviceStatus() : 3);
                scrap.setApprovalTime(LocalDateTime.now());
                //新增
                if (scrapMaintenceDto.getMaintenanceUserId() != null && scrapMaintenceDto.getRepairmanUserId() != null) {
                    Maintenance maintenance = new Maintenance();
                    maintenance.setMaintenanceUserId(scrapMaintenceDto.getMaintenanceUserId());
                    maintenance.setRepairmanUserId(scrapMaintenceDto.getRepairmanUserId());
                    maintenance.setRemarks(scrapMaintenceDto.getRemarks());
                    maintenance.setRepairTime(LocalDateTime.now());
                    maintenance.setFaultDescription(scrapMaintenceDto.getFaultDescription());

                    Device device = deviceService.getById(scrap.getDeviceId());
                    if (!ObjectUtils.isEmpty(device)) {
                        maintenance.setDeviceId(device.getDeviceId());
                        maintenance.setDeviceName(device.getDeviceName());
                        maintenance.setDeviceCode(device.getDeviceCode());
                        device.setDeviceStatus(3);
                    }
                    Department department = departmentService.getById(device.getDepartmentId());
                    if (!ObjectUtils.isEmpty(department)) {
                        maintenance.setDepartmentAddress(department.getDepartmentAddress());
                        maintenance.setDepartmentId(department.getDepartmentId());
                    }
                    //修改设备状态
                    deviceService.updateById(device);
                    //新增
                    maintenanceService.save(maintenance);
                    //审核不通过
                    scrapService.updateById(scrap);
                }

            }
        }
    }
}

