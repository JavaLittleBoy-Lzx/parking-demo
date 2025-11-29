package com.parkingmanage.controller;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import com.parkingmanage.common.AIKER;
import com.parkingmanage.common.R;
import com.parkingmanage.common.Result;
import com.parkingmanage.common.config.AIKEConfig;
import com.parkingmanage.entity.Gate;
import com.parkingmanage.query.GateQuery;
import com.parkingmanage.service.GateService;
import com.parkingmanage.utils.PageUtils;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author MLH
 * @since 2023-02-18
 */
@RestController
@RequestMapping("/parking/gate")
public class GateController {
    @Resource
    private GateService gateService;

    @Autowired
    public AIKEConfig aikeConfig;

    @GetMapping("/getTickets")
    public R getTickets(@RequestParam(required = false) List<String> parkCodeList) {
        String cmd = "getMonthTicketConfigDetailList";
        HashMap<String, Object> params = new HashMap<>();
        params.put("parkCodeList", parkCodeList);
        JSONObject data = aikeConfig.downHandler(AIKEConfig.AK_URL, AIKEConfig.AK_KEY, AIKEConfig.AK_SECRET,cmd, params);
        return R.ok().data(data);
    }

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertGate(@RequestBody Gate gate) {
        System.out.println(gate);
        int num = gateService.duplicate(gate);
        Result result = new Result();
        if (num == 0) {
            gateService.save(gate);
        } else {
            result.setCode("1");
            result.setMsg("数据重复，增加失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Gate gate) {
        int num = gateService.duplicate(gate);
        Result result = new Result();
        if (num == 0) {
            gateService.updateById(gate);
        } else {
            result.setCode("1");
            result.setMsg("数据重复，修改失败！");
        }
        return ResponseEntity.ok(result);
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return gateService.removeById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/queryGate")
    public List<Gate> queryGate(GateQuery query) {
        QueryWrapper<Gate> wrapper = Wrappers.<Gate>query();
        wrapper.eq("a.province", query.getProvince());
        wrapper.eq("a.city", query.getCity());
        wrapper.eq("a.district", query.getDistrict());
        wrapper.eq("a.community", query.getCommunity());
        wrapper.eq("a.gatename", query.getGatename());
        List<Gate> myquery = gateService.queryGate(wrapper);
        return myquery;
    }
    @ApiOperation("分页查询")
    @GetMapping("/querypage")
    public IPage<Gate> queryPage(
            @RequestParam(required = false) String gatename,
            @RequestParam(required = false, value = "community") String community,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {

        List<Gate> gateList = gateService.queryListGate(gatename, community);
        //按照设备名和申请日期排序
        List<Gate> asServices = gateList.stream().sorted(Comparator.comparing(Gate::getGatename).thenComparing(Gate::getCommunity)).collect(Collectors.toList());
        return PageUtils.getPage(asServices, pageNum, pageSize);
    }
}

