package com.parkingmanage.controller;


import com.alibaba.druid.sql.PagerUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.entity.ReportCarIn;
import com.parkingmanage.entity.VehicleReservation;
import com.parkingmanage.service.ReportCarInService;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author lzx
 * @since 2024-04-27
 */
@Slf4j
@RestController
@RequestMapping("/parking/akReportCarIn")
public class ReportCarInController {
    @Autowired
    private ReportCarInService reportCarInService;

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<ReportCarIn> findPage(@RequestParam(required = false) String enterCarLicenseNumber,
                                       @RequestParam(required = false) String yardName,
                                       @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                       @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        Page<ReportCarIn> reportCarInPage = new Page<>(pageNum, pageSize);
        QueryWrapper<ReportCarIn> reportCarInQueryWrapper = new QueryWrapper<>();
        // 模糊查询，根据车牌号码进行搜索
        if (StringUtils.isNotBlank(enterCarLicenseNumber)) {
            reportCarInQueryWrapper.like("enter_car_license_number", enterCarLicenseNumber);
        }
        if (StringUtils.isNotBlank(yardName)) {
            reportCarInQueryWrapper.like("yard_name",yardName);
        }
        return reportCarInService.page(reportCarInPage,reportCarInQueryWrapper);
    }
}

