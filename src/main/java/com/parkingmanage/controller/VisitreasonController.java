package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.R;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Department;
import com.parkingmanage.entity.Visitreason;
import com.parkingmanage.service.VisitreasonService;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 <p>
  前端控制器
 </p>

 @author MLH
 @since 2022-08-27
*/
@RestController
@RequestMapping("/parking/visitreason")
public class VisitreasonController {
    @Resource
    private VisitreasonService visitreasonService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insert(@RequestBody Visitreason visitreason) {
        visitreasonService.save(visitreason);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Visitreason visitreason) {
        visitreasonService.updateById(visitreason);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return visitreasonService.removeById(id);
    }

    @ApiOperation("查询单条")
    @GetMapping("/{id}")
    public Visitreason findById(@PathVariable String id) {
        return visitreasonService.getById(id);
    }

    @GetMapping("/getList")
    @ResponseBody
    public R<Map<String,Object>> getList(){
        Integer count=0;
        ArrayList<Object> visitreasonList = new ArrayList<>();
        List<Visitreason> visitreasonAll = visitreasonService.list();
        Iterator<Visitreason> iter = visitreasonAll.iterator();
        while (iter.hasNext()) {
            Visitreason visitreason = iter.next();
            Map<String,Object>  visitreasonMap = new HashMap<>();
            visitreasonMap.put("reason",visitreason.getReason());
            visitreasonList.add(visitreasonMap);
        }
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("data",visitreasonList);
        return R.ok(dataMap);
    }
    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<Visitreason> findPage(@RequestParam(required = false) String reason,
                                      @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                      @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        IPage<Visitreason> page = visitreasonService.page(new Page<>(pageNum, pageSize), Wrappers.<Visitreason>lambdaQuery().like(Visitreason::getReason, reason));
        List<Visitreason> visitreasons = page.getRecords().stream().sorted(Comparator.comparing(Visitreason::getSortno)).collect(Collectors.toList());
        page.setRecords(visitreasons);
        return page;
    }
}

