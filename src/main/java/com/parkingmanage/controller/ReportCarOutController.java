package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.entity.ReportCarOut;
import com.parkingmanage.service.ReportCarOutService;
import io.swagger.annotations.ApiOperation;
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
@RestController
@RequestMapping("/parking/akReportCarOut")
public class ReportCarOutController {

    @Autowired
    private ReportCarOutService reportCarOutService;

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<ReportCarOut> findPage(@RequestParam(required = false) String leaveCarLicenseNumber,
                                        @RequestParam(required = false) String yardName,
                                        @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                        @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        Page<ReportCarOut> reportCarOutPage = new Page<>(pageNum, pageSize);
        QueryWrapper<ReportCarOut> reportCarOutQueryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(leaveCarLicenseNumber)) {
            reportCarOutQueryWrapper.like("leave_car_license_number", leaveCarLicenseNumber);
        }
        if (StringUtils.isNotBlank(yardName)) {
            reportCarOutQueryWrapper.like("yard_name",yardName);
        }
        return reportCarOutService.page(reportCarOutPage,reportCarOutQueryWrapper);
    }
}

