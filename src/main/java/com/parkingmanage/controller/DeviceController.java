package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Department;
import com.parkingmanage.entity.Device;
import com.parkingmanage.service.DepartmentService;
import com.parkingmanage.service.DeviceService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 <p>
 设备信息 前端控制器
 </p>

 @author yuli
 @since 2022-03-01
*/
@RestController
@RequestMapping("/parking/device")
public class DeviceController {
    @Resource
    private DeviceService deviceService;
    @Resource
    DepartmentService departmentService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insert(@RequestBody Device device) {
        deviceService.insertDevice(device);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Device device) {
        deviceService.updateById(device);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return deviceService.removeById(id);
    }

    @ApiOperation("查询单条")
    @GetMapping("/{id}")
    public Device findById(@PathVariable String id) {
        return deviceService.getById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/listAll")
    public List<Device> findAll() {
        return deviceService.list();
    }

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<Device> findPage(@RequestParam(required = false) String deviceName,
                                  @RequestParam(required = false) String deviceCode,
                                  @RequestParam(required = false) String deviceType,
                                  @RequestParam(required = false) Integer departmentId,
                                  @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                  @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Device> devices = deviceService.queryList(deviceName, deviceCode, deviceType, departmentId);
        return PageUtils.getPage(devices, pageNum, pageSize);
    }

    @ApiOperation("审批")
    @GetMapping("/exportDevice")
    public void exportDevice(HttpServletResponse response) {
        deviceService.exportDevice(response);
    }

    @ApiOperation("根据设备状态查询所有")
    @GetMapping("/listByType")
    public List<Device> findByTypeAll() {
        return deviceService.list(Wrappers.<Device>lambdaQuery().eq(Device::getDeviceStatus, 1).or()
                .eq(Device::getDeviceStatus, 2));
    }

    @ApiOperation("根据设备状态查询所有1")
    @GetMapping("/listByTypeOne")
    public List<Device> listByTypeOne() {
        List<Device> devices = deviceService.queryList(null, null, null, null);
        devices = devices.stream().filter((Device d) -> d.getDeviceStatus() == 1).collect(Collectors.toList());
        return devices;
    }

    @GetMapping("/getDepartment")
    public Device getDepartment(String deviceId) {
        Device device = deviceService.getById(deviceId);
        Department de = departmentService.getById(device.getDepartmentId());
        device.setDepartmentName(de.getDepartmentName());
        return device;
    }

    @ApiOperation("筛选可报废设备")
    @GetMapping("/getByStatue")
    public List<Device> getByStatus() {
        List<Device> devices = deviceService.queryList(null, null, null, null);
        devices = devices.stream().filter((Device d) -> d.getDeviceStatus() == 1 || d.getDeviceStatus() == 2 || d.getDeviceStatus() == 7).collect(Collectors.toList());
        return devices;
    }
    @ApiOperation("设备台账统计报表")
    @GetMapping("/accoutNmber")
    public ResponseEntity<Result> accoutNmber() {
        List<Integer> arrayList=new ArrayList<>();
        arrayList.add(deviceService.list(Wrappers.<Device>lambdaQuery().eq(Device::getDeviceType,1)).size());
        arrayList.add(deviceService.list(Wrappers.<Device>lambdaQuery().eq(Device::getDeviceType,2)).size());
        arrayList.add(deviceService.list(Wrappers.<Device>lambdaQuery().eq(Device::getDeviceType,3)).size());
        arrayList.add(deviceService.list(Wrappers.<Device>lambdaQuery().eq(Device::getDeviceType,4)).size());
        arrayList.add(deviceService.list(Wrappers.<Device>lambdaQuery().eq(Device::getDeviceType,5)).size());
        arrayList.add(deviceService.list(Wrappers.<Device>lambdaQuery().eq(Device::getDeviceType,6)).size());

        return ResponseEntity.ok(new Result(arrayList));
    }

}

