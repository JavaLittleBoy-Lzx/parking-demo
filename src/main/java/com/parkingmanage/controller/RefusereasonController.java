package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Refusereason;
import com.parkingmanage.entity.Visitreason;
import com.parkingmanage.service.RefusereasonService;
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
 @since 2022-09-05
*/
@RestController
@RequestMapping("/parking/refusereason")
public class RefusereasonController {
    @Resource
    private RefusereasonService refusereasonService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insert(@RequestBody Refusereason refusereason) {
        refusereasonService.save(refusereason);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Refusereason refusereason) {
        refusereasonService.updateById(refusereason);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return refusereasonService.removeById(id);
    }

    @ApiOperation("查询单条")
    @GetMapping("/{id}")
    public Refusereason findById(@PathVariable String id) {
        return refusereasonService.getById(id);
    }
    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<Refusereason> findPage(@RequestParam(required = false) String reason,
                                       @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                       @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        IPage<Refusereason> page = refusereasonService.page(new Page<>(pageNum, pageSize), Wrappers.<Refusereason>lambdaQuery().like(Refusereason::getReason, reason));
        List<Refusereason> refusereason = page.getRecords().stream().sorted(Comparator.comparing(Refusereason::getSortno)).collect(Collectors.toList());
        page.setRecords(refusereason);
        return page;
    }

    @GetMapping("/getList")
    @ResponseBody
    public R<Map<String,Object>> getList(){
        Integer count=0;
        ArrayList<Object> refusereasonList = new ArrayList<>();
        List<Refusereason> refusereasonAll = refusereasonService.list();
        Iterator<Refusereason> iter = refusereasonAll.iterator();
        while (iter.hasNext()) {
            Refusereason refusereason = iter.next();
            Map<String,Object>  refusereasonMap=new HashMap<>();
            refusereasonMap.put("reason",refusereason.getReason());
            refusereasonList.add(refusereasonMap);
        }
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("data",refusereasonList);
        return R.ok(dataMap);
    }
}

