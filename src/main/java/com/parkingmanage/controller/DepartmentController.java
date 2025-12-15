package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Department;
import com.parkingmanage.service.DepartmentService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 <p>
 部门管理 前端控制器
 </p>

 @author yuli
 @since 2022-02-27
*/
@RestController
@RequestMapping("/parking/department")
@Api(tags = "部门管理")
public class DepartmentController {

    @Resource
    private DepartmentService departmentService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertRole(@RequestBody Department department) {
        departmentService.save(department);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Department department) {
        departmentService.updateById(department);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return departmentService.removeById(id);
    }

    @ApiOperation("查询单条")
    @GetMapping("/{id}")
    public Department findById(@PathVariable String id) {
        return departmentService.getById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/listDepartment")
    public List<Department> findAll() {
        return departmentService.list();
    }

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<Department> findPage(@RequestParam(required = false) String name,
                                      @RequestParam(required = false) String leader,
                                      @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                      @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        IPage<Department> page = departmentService.page(new Page<>(pageNum, pageSize), Wrappers.<Department>lambdaQuery().like(Department::getDepartmentName, name)
                .like(Department::getLeader, leader));
        List<Department> departments = page.getRecords().stream().sorted(Comparator.comparing(Department::getDepartmentName)).collect(Collectors.toList());
        page.setRecords(departments);
        return page;
    }

}

