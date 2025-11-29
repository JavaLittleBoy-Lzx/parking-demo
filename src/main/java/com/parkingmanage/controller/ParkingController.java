package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Parking;
import com.parkingmanage.service.ParkingService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 <p>
  前端控制器
 </p>

 @author MLH
 @since 2022-11-05
*/
@RestController
@RequestMapping("/parking/parking")
public class ParkingController {
    @Resource
    private ParkingService parkingService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertallocation(@RequestBody Parking parking) {
        parkingService.save(parking);
        return ResponseEntity.ok(new Result());
    }
    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Parking parking) {
        parkingService.updateById(parking);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return parkingService.removeById(id);
    }

    @ApiOperation("分页查询")
    @GetMapping("/allpage")
    public IPage<Parking> allPage(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String community,
            @RequestParam(required = false) String building,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Parking> parkingList = parkingService.getList(province,city,district,community);
        //按照设备名和申请日期排序
        List<Parking> asServices = parkingList.stream().sorted(Comparator.comparing(Parking::getProvince).
                thenComparing(Parking::getCity).thenComparing(Parking::getDistrict).thenComparing(Parking::getCommunity)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }
}