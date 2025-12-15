package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.VehicleClassification;
import com.parkingmanage.entity.YardInfo;
import com.parkingmanage.service.VehicleClassificationService;
import com.parkingmanage.service.YardInfoService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 李子雄
 */
@RestController
@RequestMapping("/parking/vehicleClassification")
public class VehicleClassificationController {
    @Resource
    private VehicleClassificationService vehicleClassificationService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertVehicleClassification(@RequestBody VehicleClassification vehicleClassification) {
        int num = vehicleClassificationService.duplicate(vehicleClassification);
        Result result = new Result();
        if (num == 0) {
            vehicleClassificationService.save(vehicleClassification);
        } else {
            result.setCode("1");
            result.setMsg("数据重复，增加失败！");
        }

        return ResponseEntity.ok(result);
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody VehicleClassification vehicleClassification) {
        int num = vehicleClassificationService.duplicate(vehicleClassification);
        Result result = new Result();
        if (num == 0) {
            vehicleClassificationService.updateById(vehicleClassification);
        } else {
            result.setCode("1");
            result.setMsg("数据重复，修改失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return vehicleClassificationService.removeById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/getAllVehicleClassification")
    public List<VehicleClassification> getAllVehicleClassification() {
        List<VehicleClassification> myquery = vehicleClassificationService.list();
        return myquery;
    }

    @ApiOperation("查询所有车辆分类")
    @GetMapping("/vehicleClassification")
    public List<VehicleClassification> vehicleClassificationList() {
        return vehicleClassificationService.vehicleClassificationList();
    }

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<VehicleClassification> findPage(@RequestParam(required = false) String vehicleClassification,
                                                 @RequestParam(required = false) String classificationNo,
                                                 @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                                 @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<VehicleClassification> vehicleClassificationList = vehicleClassificationService.queryListVehicleClassification(vehicleClassification, classificationNo);
        //按照设备名和申请日期排序
        List<VehicleClassification> asServices = vehicleClassificationList.stream().sorted(Comparator.comparing(VehicleClassification::getVehicleClassification).thenComparing(VehicleClassification::getClassificationNo)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }
}

