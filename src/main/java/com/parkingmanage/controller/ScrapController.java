package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Device;
import com.parkingmanage.entity.Scrap;
import com.parkingmanage.service.DeviceService;
import com.parkingmanage.service.ScrapService;
import com.parkingmanage.utils.PageUtils;
import com.parkingmanage.vo.ScrapMaintenceDto;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 <p>
 报废管理 前端控制器
 </p>

 @author yuli
 @since 2022-03-04
*/
@RestController
@RequestMapping("/parking/scrap")
public class ScrapController {


    @Resource
    private ScrapService scrapService;
    @Resource
    private DeviceService deviceService;

    @ApiOperation("添加/")
    @PostMapping
    public ResponseEntity<Result> insertScrap(@RequestBody Scrap scrap) {
        scrapService.saveScrap(scrap);
        // scrapService.save(scrap);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Scrap scrap) {
        // scrapService.updateScrapAndDevice(scrap);
        scrapService.updateById(scrap);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        Scrap scrap = scrapService.getById(id);
        Device device = deviceService.getById(scrap.getDeviceId());
        if (ObjectUtils.isNotEmpty(device)) {
            device.setDeviceStatus(device.getOriginalState());
            deviceService.updateById(device);
        }
        return scrapService.removeById(id);
    }

    @ApiOperation("查询单条")
    @GetMapping("/{id}")
    public Scrap findById(@PathVariable String id) {
        return scrapService.getById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/list")
    public List<Scrap> findAll() {
        return scrapService.list();
    }

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<Scrap> findPage(@RequestParam(required = false) String deviceName,
                                 @RequestParam(required = false, value = "deviceCode") String deviceCode,
                                 @RequestParam(required = false, value = "scrapDate") String scrapDate,
                                 @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                 @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Scrap> scrapList = scrapService.queryListScrap(deviceName, scrapDate, deviceCode);
        //按照设备名和申请日期排序
        scrapList = scrapList.stream().sorted(Comparator.comparing(Scrap::getDeviceName).thenComparing(Scrap::getScrapDate)).collect(Collectors.toList());
        return PageUtils.getPage(scrapList, pageNum, pageSize);
    }

    @GetMapping("/exportScrap")
    public void exportScrap(@RequestParam(required = false) String deviceName,
                            @RequestParam(required = false, value = "deviceCode") String deviceCode,
                            @RequestParam(required = false, value = "scrapDate") String scrapDate,
                            HttpServletResponse response) {
        scrapService.exportScrap(deviceName, deviceCode, scrapDate, response);
    }
    @ApiOperation("分页查询")
    @GetMapping("/mypage")
    public IPage<Scrap> myFindPage(
            @RequestParam(value = "audiusUserId") Integer audiusUserId,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false, value = "deviceCode") String deviceCode,
            @RequestParam(required = false, value = "scrapDate") String scrapDate,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Scrap> scrapList = scrapService.queryListScrap(deviceName, scrapDate, deviceCode);
      //  scrapList = scrapList.stream().filter((Scrap s) -> s.getAudiusUserId().equals(audiusUserId)).collect(Collectors.toList());
        //按照设备名和申请日期排序
        List<Scrap> asServices = scrapList.stream().sorted(Comparator.comparing(Scrap::getDeviceName).thenComparing(Scrap::getScrapDate)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }

    @ApiOperation("审核通过")
    @PutMapping("/auditScrap")
    public ResponseEntity<Result> auditScrap(@RequestBody Scrap scrap) {
        scrapService.updateScrapAndDevice(scrap);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("审核未通过")
    @PutMapping("/auditScrapAndMaintence")
    public ResponseEntity<Result> auditScrapAndMaintence(@RequestBody ScrapMaintenceDto scrapMaintenceDto) {
        scrapService.auditScrapAndMaintence(scrapMaintenceDto);
        return ResponseEntity.ok(new Result());
    }
}