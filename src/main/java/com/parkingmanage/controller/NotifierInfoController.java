package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.NotifierInfo;
import com.parkingmanage.entity.VehicleReservation;
import com.parkingmanage.entity.YardInfo;
import com.parkingmanage.service.NotifierInfoService;
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
 *  前端控制器
 * </p>
 *
 * @author 李子雄
 *
 */
@RestController
@RequestMapping("/parking/notifierInfo")
public class NotifierInfoController {
    @Resource
    private NotifierInfoService notifierInfoService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertNotifierInfoService(@RequestBody NotifierInfo notifierInfo) {
        int num = notifierInfoService.duplicate(notifierInfo);
        Result result = new Result();
        if (num == 0) {
            notifierInfoService.save(notifierInfo);

        } else {
            result.setCode("1");
            result.setMsg("数据重复，增加失败！");
        }

        return ResponseEntity.ok(result);
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody NotifierInfo notifierInfo) {
        int num = notifierInfoService.duplicate(notifierInfo);
        Result result = new Result();
        if (num == 0) {
            notifierInfoService.updateById(notifierInfo);
        } else {
            result.setCode("1");
            result.setMsg("数据重复，修改失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return notifierInfoService.removeById(id);
    }

    @ApiOperation("查询所有商户名称")
    @GetMapping("/merchantName")
    public List<NotifierInfo> merchantNameList() {
        return notifierInfoService.merchantNameList();
    }

    @ApiOperation("查询所有通知人姓名")
    @GetMapping("/notifierName")
    public List<NotifierInfo> notifierNameList(@RequestParam(required = false) String merchantName) {
        return notifierInfoService.notifierNameList(merchantName);
    }
    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<NotifierInfo> findPage(@RequestParam(required = false) String merchantName,
                                              @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                              @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<NotifierInfo> notifierInfoList = notifierInfoService.queryListNotifierInfo(merchantName);
        //按照设备名和申请日期排序
        List<NotifierInfo> asServices = notifierInfoList.stream().sorted(Comparator.comparing(NotifierInfo::getMerchantName)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }

}

