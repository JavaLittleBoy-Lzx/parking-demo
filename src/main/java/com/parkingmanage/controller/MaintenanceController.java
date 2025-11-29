package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Device;
import com.parkingmanage.entity.Maintenance;
import com.parkingmanage.service.DeviceService;
import com.parkingmanage.service.MaintenanceService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.stream.Collectors;

/**
 <p>
 维修管理 前端控制器
 </p>

 @author yuli
 @since 2022-03-01
*/
@RestController
@RequestMapping("/parking/maintenance")
public class MaintenanceController {
    @Resource
    private MaintenanceService maintenanceService;
    @Resource
    private DeviceService deviceService;


    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insert(@RequestBody Maintenance maintenance) {
        maintenanceService.insertMaintenance(maintenance);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Maintenance maintenance) {
        maintenanceService.updateById(maintenance);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        Maintenance maintenance = maintenanceService.getById(id);
        Device device = deviceService.getById(maintenance.getDeviceId());
        if (ObjectUtils.isNotEmpty(device)) {
            device.setDeviceStatus(device.getOriginalState());
            deviceService.updateById(device);
        }
        return maintenanceService.removeById(id);
    }

    @ApiOperation("查询单条")
    @GetMapping("/{id}")
    public Maintenance findById(@PathVariable String id) {
        return maintenanceService.getById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/listAll")
    public List<Maintenance> findAll() {
        return maintenanceService.list();
    }

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<Maintenance> findPage(@RequestParam(required = false) String deviceName,
                                       @RequestParam(required = false, value = "departmentId") String departmentId,
                                       @RequestParam(required = false, value = "deviceCode") String deviceCode,
                                       @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                       @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Maintenance> maintenances = maintenanceService.queryMaintenanceList(deviceName, departmentId, deviceCode);
        return PageUtils.getPage(maintenances, pageNum, pageSize);
    }

    @ApiOperation("审批")
    @GetMapping("/exportMaintenance")
    public void exportPurchaseManagement(HttpServletResponse response) {
        maintenanceService.exportMaintenance(response);
    }

    @ApiOperation("修改")
    @PostMapping("/updateManage")
    public ResponseEntity<Result> updateManage(@RequestBody Maintenance maintenance) {
        if (ObjectUtils.isNotEmpty(maintenance) && maintenance.getDeviceId() != null) {
            Device device = deviceService.getById(maintenance.getDeviceId());
            if (maintenance.getAuditStatus() != null && maintenance.getAuditStatus() == 2) {
                device.setDeviceStatus(1);
            }
            if (maintenance.getAuditStatus() != null && maintenance.getAuditStatus() == 3) {
                device.setDeviceStatus(7);//报废中
            }
            deviceService.updateById(device);
            maintenanceService.updateById(maintenance);
        }
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("分页查询")
    @GetMapping("/pageBymaintenanceUserId")
    public IPage<Maintenance> queryPage(@RequestParam(required = false) String deviceName,
                                        @RequestParam(required = false, value = "departmentId") String departmentId,
                                        @RequestParam(required = false, value = "deviceCode") String deviceCode,
                                        @RequestParam( value = "maintenanceUserId") Integer maintenanceUserId,
                                        @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                        @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Maintenance> maintenances = maintenanceService.queryMaintenanceList(deviceName, departmentId, deviceCode);
        maintenances=  maintenances.stream().filter((Maintenance m) ->  m.getMaintenanceUserId().equals(maintenanceUserId)).collect(Collectors.toList());
        return PageUtils.getPage(maintenances, pageNum, pageSize);
    }

    @ApiOperation("最近三个月设备维修情况")
    @GetMapping("/queryMaintenance")
    public ResponseEntity<Result> queryMaintenance() {
        return ResponseEntity.ok(new Result(maintenanceService.queryMaintenance()));
    }
}

