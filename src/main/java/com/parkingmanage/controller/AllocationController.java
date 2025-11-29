package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Allocation;
import com.parkingmanage.entity.Device;
import com.parkingmanage.service.AllocationService;
import com.parkingmanage.service.DeviceService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 <p>
 调拨管理 前端控制器
 </p>

 @author yuli
 @since 2022-03-02
*/
@RestController
@RequestMapping("/parking/allocation")
public class AllocationController {
    @Resource
    private AllocationService allocationService;
    @Resource
    private DeviceService deviceService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertallocation(@RequestBody Allocation allocation) {
        allocationService.saveAllocation(allocation);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Allocation allocation) {
        allocationService.updateById(allocation);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        Allocation allocation = allocationService.getById(id);
        Device device = deviceService.getById(allocation.getDeviceId());
        if (ObjectUtils.isNotEmpty(device)) {
            device.setDeviceStatus(device.getOriginalState());
            deviceService.updateById(device);
        }
        return allocationService.removeById(id);
    }

    @ApiOperation("查询单条")
    @GetMapping("/{id}")
    public Allocation findById(@PathVariable String id) {
        return allocationService.getById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/listallocation")
    public List<Allocation> findAll() {
        return allocationService.list();
    }

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<Allocation> findPage(@RequestParam(required = false) String deviceName,
                                      @RequestParam(required = false) String deviceCode,
                                      @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                      @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Allocation> allocationList = allocationService.queryList(deviceName, deviceCode);
        return PageUtils.getPage(allocationList, pageNum, pageSize);
    }

    @GetMapping("/exportAllocation")
    public void exportAllocation(@RequestParam(required = false) String deviceName,
                                 @RequestParam(required = false) String deviceCode, HttpServletResponse response) {
        allocationService.exportAllocation(deviceName, deviceCode, response);
    }

    @ApiOperation("分页查询")
    @GetMapping("/pageByAuditUserId")
    public IPage<Allocation> queryPage(@RequestParam(required = false) String deviceName,
                                       @RequestParam(required = false, value = "deviceCode") String deviceCode,
                                       @RequestParam(value = "auditUserId") Integer auditUserId,
                                       @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                       @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Allocation> allocationList = allocationService.queryList(deviceName, deviceCode);
        allocationList = allocationList.stream().filter((Allocation m) -> m.getAuditUserId().equals(auditUserId)).collect(Collectors.toList());
        return PageUtils.getPage(allocationList, pageNum, pageSize);
    }

    @ApiOperation("审批")
    @PostMapping("/updateAllocation")
    public ResponseEntity<Result> updateAllocation(@RequestBody Allocation allocation) {
        if (ObjectUtils.isNotEmpty(allocation) && allocation.getDeviceId() != null) {
            Device device = deviceService.getById(allocation.getDeviceId());
            if (allocation.getAuditStatus() != null && allocation.getAuditStatus() == 2) {
                device.setDeviceStatus(2);
                device.setDepartmentId(allocation.getAfterDepartmentId());
            }
            if (allocation.getAuditStatus() != null && allocation.getAuditStatus() == 3) {
                device.setDeviceStatus(device.getOriginalState());
            }
            deviceService.updateById(device);
            allocation.setAuditusTime(LocalDateTime.now());
            allocationService.updateById(allocation);
        }
        return ResponseEntity.ok(new Result());
    }
}

