package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Department;
import com.parkingmanage.entity.Device;
import com.parkingmanage.entity.Rental;
import com.parkingmanage.entity.User;
import com.parkingmanage.service.DepartmentService;
import com.parkingmanage.service.DeviceService;
import com.parkingmanage.service.RentalService;
import com.parkingmanage.service.UserService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 <p>
 * 设备租赁 前端控制器
 * </p>
 *
 * @author yuli
 * @since 2022-03-02
 */
@RestController
@RequestMapping("/parking/rental")
public class RentalController {
    @Resource
    private RentalService rentalService;
    @Resource
    private DeviceService deviceService;
    @Resource
    private UserService userService;
    @Resource
    private DepartmentService departmentService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insert(@RequestBody Rental rental) {
        rentalService.insertRental(rental);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("审核")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Rental rental) {
        if (ObjectUtils.isNotEmpty(rental) && rental.getAudiusStatus() != null) {
            Device device = deviceService.getById(rental.getDeviceId());
            if (ObjectUtils.isNotEmpty(device)) {
            if (rental.getAudiusStatus() == 3) {
                    device.setDeviceStatus(device.getOriginalState());
                }else if (rental.getAudiusStatus() == 2) {
                device.setDeviceStatus(4);
                }
            }
            deviceService.updateById(device);
        }
        rentalService.updateById(rental);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        Rental rental = rentalService.getById(id);
        Device device = deviceService.getById(rental.getDeviceId());
        if (ObjectUtils.isNotEmpty(device)) {
            device.setDeviceStatus(device.getOriginalState());
            deviceService.updateById(device);
        }
        return rentalService.removeById(id);
    }

    @ApiOperation("查询单条")
    @GetMapping("/{id}")
    public Rental findById(@PathVariable String id) {
        return rentalService.getById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/listAll")
    public List<Rental> findAll() {
        return rentalService.list();
    }

    @ApiOperation("申请分页查询")
    @GetMapping("/page")
    public IPage<Rental> findPage(@RequestParam(required = false) String deviceName,
                                  @RequestParam(required = false, value = "customerName") String customerName,
                                  @RequestParam(required = false, value = "deviceCode") String deviceCode,
                                  @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                  @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Rental> rentals = rentalService.list(Wrappers.<Rental>lambdaQuery()
                .like(StringUtils.hasLength(deviceName), Rental::getDeviceName, deviceName)
                .like(StringUtils.hasLength(customerName), Rental::getCustomerName, customerName)
                .like(StringUtils.hasLength(deviceCode), Rental::getDeviceCode, deviceCode));
        for (Rental rental : rentals) {
            User user = userService.getById(rental.getApplicantUserId());
            if (user != null) {
                rental.setApplicantUserName(user.getUserName());
            }
            User us = userService.getById(rental.getAudiusUserId());
            if (us != null) {
                rental.setAudiusUserName(us.getUserName());
            }
            Department department = departmentService.getById(rental.getDepartmentId());
            if (department != null) {
                rental.setDepartmentName(department.getDepartmentName());
            }
        }
        return PageUtils.getPage(rentals, pageNum, pageSize);
    }

    @GetMapping("/exportAllocation")
    public void exportAllocation(@RequestParam(required = false) String deviceName,
                                 @RequestParam(required = false, value = "customerName") String customerName,
                                 @RequestParam(required = false, value = "deviceCode") String deviceCode,
                                 HttpServletResponse response) {
        rentalService.exportRental(deviceName, customerName, deviceCode, response);
    }

    @ApiOperation("审核分页查询")
    @GetMapping("/mypage")
    public IPage<Rental> myPage(@RequestParam(required = false) String deviceName,
                                @RequestParam(required = true) String audiusUserId,
                                @RequestParam(required = false, value = "customerName") String customerName,
                                @RequestParam(required = false, value = "deviceCode") String deviceCode,
                                @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Rental> rentals = rentalService.list(Wrappers.<Rental>lambdaQuery()
                .eq(StringUtils.hasLength(audiusUserId), Rental::getAudiusUserId, audiusUserId)
                .like(StringUtils.hasLength(deviceName), Rental::getDeviceName, deviceName)
                .like(StringUtils.hasLength(customerName), Rental::getCustomerName, customerName)
                .like(StringUtils.hasLength(deviceCode), Rental::getDeviceCode, deviceCode));
        for (Rental rental : rentals) {

            User user = userService.getById(rental.getApplicantUserId());
            if (user != null) {
                rental.setApplicantUserName(user.getUserName());
            }
            User us = userService.getById(rental.getAudiusUserId());
            if (us != null) {
                rental.setAudiusUserName(us.getUserName());
            }
            Department department = departmentService.getById(rental.getDepartmentId());
            if (department != null) {
                rental.setDepartmentName(department.getDepartmentName());
            }
        }
        return PageUtils.getPage(rentals, pageNum, pageSize);
    }

    @ApiOperation("归还")
    @PutMapping("/updateTime")
    public ResponseEntity<Result> updateTime(@RequestBody Rental rental) {
        rentalService.updateTime(rental);
        return ResponseEntity.ok(new Result());
    }
    @ApiOperation("最近3个月各类设备租赁趋势图")
    @GetMapping("/queryRental")
    public ResponseEntity<Result> queryRental() {

        return ResponseEntity.ok(new Result(rentalService.queryRental()));
    }

}

