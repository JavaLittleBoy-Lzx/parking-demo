package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.ReleaseReason;
import com.parkingmanage.entity.VehicleReservation;
import com.parkingmanage.entity.YardInfo;
import com.parkingmanage.service.RefusereasonService;
import com.parkingmanage.service.ReleaseReasonService;
import com.parkingmanage.service.YardInfoService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.management.relation.RelationService;
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
@RequestMapping("/parking/releaseReason")
public class ReleaseReasonController {
    @Resource
    private ReleaseReasonService releaseReasonService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertRelationService(@RequestBody ReleaseReason releaseReason) {
        int num = releaseReasonService.duplicate(releaseReason);
        Result result = new Result();
        if (num == 0) {
            releaseReasonService.save(releaseReason);

        } else {
            result.setCode("1");
            result.setMsg("数据重复，增加失败！");
        }

        return ResponseEntity.ok(result);
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody ReleaseReason releaseReason) {
        int num = releaseReasonService.duplicate(releaseReason);
        Result result = new Result();
        if (num == 0) {
            releaseReasonService.updateById(releaseReason);
        } else {
            result.setCode("1");
            result.setMsg("数据重复，修改失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return releaseReasonService.removeById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/getAllReleaseReason")
    public List<ReleaseReason> getAllReleaseReason() {
        List<ReleaseReason> myquery = releaseReasonService.list();
        return myquery;
    }
    @ApiOperation("查询所有放行原因")
    @GetMapping("/releaseReason")
    public List<ReleaseReason> releaseReasonList() {
        return releaseReasonService.releaseReasonList();
    }

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<ReleaseReason> findPage(@RequestParam(required = false) String releaseReason,
                                              @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                              @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<ReleaseReason> releaseReasonList = releaseReasonService.queryListReleaseReason(releaseReason);
        //按照设备名和申请日期排序
        List<ReleaseReason> asServices = releaseReasonList.stream().sorted(Comparator.comparing(ReleaseReason::getReleaseReason)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }
}

