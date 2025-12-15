package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Supplier;
import com.parkingmanage.service.SupplierService;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 <p>
 供应商管理 前端控制器
 </p>

 @author yuli
 @since 2022-02-27
*/
@RestController
@RequestMapping("/parking/supplier")
public class SupplierController {
    @Resource
    private SupplierService supplierService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertRole(@RequestBody Supplier supplier) {
        supplier.setRegistrationTime(LocalDateTime.now());
        supplierService.save(supplier);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Supplier supplier) {
        supplierService.updateById(supplier);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return supplierService.removeById(id);
    }

    @ApiOperation("查询单条")
    @GetMapping("/{id}")
    public Supplier findById(@PathVariable String id) {
        return supplierService.getById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/listsupplier")
    public List<Supplier> findAll() {
        return supplierService.list();
    }

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<Supplier> findPage(@RequestParam(required = false ) String name,
                                    @RequestParam(required = false) String supplierCode,
                                    @RequestParam(required = false) String contactsPerson,
                                    @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                    @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        IPage<Supplier> page = supplierService.page(new Page<>(pageNum, pageSize), Wrappers.<Supplier>lambdaQuery().like(StringUtils.hasLength(name),Supplier::getSupplierName, name)
                .like(StringUtils.hasLength(supplierCode),Supplier::getSupplierCode, supplierCode)
                .like(StringUtils.hasLength(contactsPerson),Supplier ::getContactsPerson,contactsPerson ));
        List<Supplier> suppliers = page.getRecords().stream().sorted(Comparator.comparing(Supplier::getSupplierName)).collect(Collectors.toList());
        page.setRecords(suppliers);
        return page;
    }
}

