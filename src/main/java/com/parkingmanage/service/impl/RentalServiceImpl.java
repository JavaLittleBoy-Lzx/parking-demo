package com.parkingmanage.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.common.exception.CustomException;
import com.parkingmanage.entity.*;
import com.parkingmanage.mapper.RentalMapper;
import com.parkingmanage.service.*;
import com.parkingmanage.vo.DeviceRentVo;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 <p>
 设备租赁 服务实现类
 </p>

 @author yuli
 @since 2022-03-02
*/
@Service
public class RentalServiceImpl extends ServiceImpl<RentalMapper, Rental> implements RentalService {
    @Resource
    private DeviceService deviceService;
    @Resource
    private UserService userService;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private CustomerService customerService;
    @Resource
    private RentalMapper rentalMapper;

    @Override
    public void insertRental(Rental rental) {
        rental.setLeaseTime(LocalDateTime.now());
        if (rental != null && rental.getDeviceId() != null && rental.getCustomerId() != null && rental.getDayPrice() != null) {
            Device device = deviceService.getById(rental.getDeviceId());
            if (device != null) {
                rental.setDeviceName(device.getDeviceName());
                rental.setDeviceCode(device.getDeviceCode());
                rental.setDeviceId(device.getDeviceId());
                rental.setDepartmentId(device.getDepartmentId());
                rental.setDeviceType(device.getDeviceType());
                device.setOriginalState(device.getDeviceStatus());
                device.setDeviceStatus(8);
            }
            Customer customer = customerService.getById(rental.getCustomerId());
            if (customer != null) {
                rental.setCustomerId(customer.getCustomerId());
                rental.setCustomerCode(customer.getCustomerCode());
                rental.setCustomerName(customer.getCustomerName());
                rental.setTelephone(customer.getTelephone());
                // rental.setCustomerAddress(customer.get());
            }
            if (rental.getLeaseTime() != null && rental.getExpectedReturn() != null) {
                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date fomatDate1 = null;
                Date fomatDate2 = null;
                try {
                    fomatDate1 = sdf.parse(df.format(rental.getLeaseTime()));
                    fomatDate2 = sdf.parse(df.format(rental.getExpectedReturn()));
                    int result = fomatDate2.compareTo(fomatDate1);
                    if (result < 0) {
                        throw new CustomException("99", "租赁时间选择错误");
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
//比较两个日期
                long days = getDaySub(df.format(rental.getLeaseTime()), df.format(rental.getExpectedReturn()));
                if (days > 0) {
                    // long balanceDay = 100;
                    // long balanceYear = 2500;
                    long monthMoney = days / 30;
                    long dayMoney = days % 30;

                    BigDecimal money = rental.getDayPrice().multiply(BigDecimal.valueOf(0.8)).multiply(new BigDecimal(monthMoney).multiply(new BigDecimal(30))).add(rental.getDayPrice().multiply(new BigDecimal(dayMoney)));
                    rental.setRent(money);
                }
                rental.setLeaseTime(LocalDateTime.now());
                deviceService.updateById(device);
                this.saveOrUpdate(rental);
            } else {
                throw new CustomException("99", "时间必填");
            }

        }
    }

    @Override
    public void updateTime(Rental rental) {
        if (rental != null && rental.getDeviceId() != null && rental.getRent() != null) {
            Device device = deviceService.getById(rental.getDeviceId());
            if (device != null) {
                device.setDeviceStatus(1);
            }
            if (rental.getLeaseTime() != null && rental.getExpectedReturn() != null && rental.getActualReturn() != null) {
                DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date fomatDate1 = null;
                Date fomatDate2 = null;
                Date fomatDate3 = null;
                try {
                    fomatDate1 = sdf.parse(df.format(rental.getLeaseTime()));
                    fomatDate2 = sdf.parse(df.format(rental.getExpectedReturn()));
                    fomatDate3 = sdf.parse(df.format(rental.getActualReturn()));
                    int result = fomatDate3.compareTo(fomatDate1);
                    if (result < 0) {
                        throw new CustomException("99", "租赁归还时间选择错误");
                    }
                    int resultTime = fomatDate3.compareTo(fomatDate2);
                    if (resultTime > 0) {
                        // long balanceDay = 150;
                        long days = getDaySub(df.format(rental.getExpectedReturn()), df.format(rental.getActualReturn()));

                        //求和
                        rental.setActualRent(new BigDecimal(days - 1).multiply(new BigDecimal("1.5").multiply(rental.getDayPrice())).add(rental.getRent()));
                    } else {
                        rental.setActualRent(rental.getRent());
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }
                deviceService.updateById(device);
                this.updateById(rental);
            }
        }
    }

    @Override
    public DeviceRentVo queryRental() {
        DeviceRentVo deviceRentVo = new DeviceRentVo();
        List<Rental> rentals = rentalMapper.getByTypeRent();
        if (!CollectionUtils.isEmpty(rentals)) {
            Map<Integer, List<Rental>> deviceType = rentals.stream().collect(Collectors.groupingBy(Rental::getDeviceType));
            List<String> typeName = new ArrayList<>();
            List<Integer> rentalNum = new ArrayList<>();
            List<BigDecimal> accrentRent = new ArrayList<>();
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
                int rentNum = 0;
                for (Rental rental : value) {
                    rentNum += rental.getActualRent().intValue();
                }
                accrentRent.add(new BigDecimal(rentNum).divide(new BigDecimal(10000)));
            });
            deviceRentVo.setAllAccentRent(accrentRent);
            deviceRentVo.setTypeName(typeName);
            deviceRentVo.setRentalNum(rentalNum);
        }
        return deviceRentVo;
    }

    @Override
    public void exportRental(String deviceName, String customerName, String deviceCode, HttpServletResponse response) {
        List<Rental> rentals = this.list(Wrappers.<Rental>lambdaQuery()
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
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("deviceName", "设备名称");
        writer.addHeaderAlias("deviceCode", "设备编码");
        writer.addHeaderAlias("departmentName", "使用部门");
        writer.addHeaderAlias("customerName", "客户名称");
        writer.addHeaderAlias("customerCode", "客户编码");
        writer.addHeaderAlias("telephone", "客户电话");
        writer.addHeaderAlias("rent", "预租金");
        writer.addHeaderAlias("leaseTime", "租赁时间");
        writer.addHeaderAlias("audiusUserName", "审批人");
        writer.addHeaderAlias("applicantUserName", "申请人");
        writer.addHeaderAlias("expectedReturn", "预计归还时间");
        writer.addHeaderAlias("remarks", "备注");
        writer.setOnlyAlias(true);
        writer.write(rentals, true);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=rentalsListexcel.xlsx");
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


    public static long getDaySub(String beginDateStr, String endDateStr) {
        long day = 0L;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        Date beginDate = null;
        Date endDate = null;
        try {
            beginDate = format.parse(beginDateStr);
            endDate = format.parse(endDateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        day = (endDate.getTime() - beginDate.getTime()) / 86400000L;
        //todo 日期1-31统计维度为31天
        //   day = day + 1;
        return day + 1;

    }
}
