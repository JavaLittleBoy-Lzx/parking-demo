package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Patrol;
import com.parkingmanage.service.CommunityService;
import com.parkingmanage.service.PatrolService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 * @author MLH
 * @since 2023-02-11
 */
@RestController
@RequestMapping("/parking/patrol")
public class PatrolController {
    @Resource
    private PatrolService patrolService;
    @Resource
    private CommunityService communityService;

    @ApiOperation("查询单条")
    @GetMapping("/{openid}")
    public ResponseEntity<Result> findByOpenid(@PathVariable String openid) {
        Patrol patrol = patrolService.getPatrolByOpenId(openid);
        Result result = new Result();
        result.setData(patrol);
        return ResponseEntity.ok(result);
    }
    @ApiOperation("查询单条")
    @GetMapping("/getById")
    public ResponseEntity<Result> getById(@RequestParam(required = false) String id) {
        Patrol patrol = patrolService.getById(id);
        Result result = new Result();
        result.setData(patrol);
        return ResponseEntity.ok(result);
    }

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertPatrol(@RequestBody Patrol patrol) {
        int num = patrolService.duplicate(patrol);
        Result result = new Result();
        if (num == 0) {
            patrol.setCreatedate(LocalDateTime.now());
            patrol.setStatus("待确认");
            patrolService.save(patrol);
        } else {
            result.setCode("1");
            result.setMsg("数据重复，增加失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Patrol patrol) {
        int num = patrolService.duplicate(patrol);
        Result result = new Result();
        if (num == 0) {
            patrolService.updateById(patrol);
        } else {
            result.setCode("1");
            result.setMsg("数据重复，修改失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return patrolService.removeById(id);
    }

    @ApiOperation("分页查询")
    @GetMapping("/querypage")
    public IPage<Patrol> queryPage(
            @RequestParam(required = false) String username,
            @RequestParam(required = false, value = "community") String community,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<Patrol> ownerList = patrolService.queryListPatrol(username, community);
        //按照设备名和申请日期排序
        List<Patrol> asServices = ownerList.stream().sorted(Comparator.comparing(Patrol::getUsername).thenComparing(Patrol::getCommunity)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }
}

