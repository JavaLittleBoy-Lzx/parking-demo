package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Customer;
import com.parkingmanage.service.CustomerService;
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
 客户管理 前端控制器
 </p>

 @author yuli
 @since 2022-02-28
*/
@RestController
@RequestMapping("/parking/customer")
public class CustomerController {
    @Resource
    private CustomerService customerService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertCustomer(@RequestBody Customer customer) {
        customer.setRegistionDate(LocalDateTime.now());
        customerService.save(customer);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Customer customer) {
        customerService.updateById(customer);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return customerService.removeById(id);
    }

    @ApiOperation("查询单条")
    @GetMapping("/{id}")
    public Customer findById(@PathVariable String id) {
        return customerService.getById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/listCustomer")
    public List<Customer> findAll() {
        return customerService.list();
    }

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public IPage<Customer> findPage(@RequestParam(required = false) String name,
                                      @RequestParam(required = false) String code,
                                      @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                      @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        IPage<Customer> page = customerService.page(new Page<>(pageNum, pageSize), Wrappers.<Customer>lambdaQuery().like(StringUtils.hasLength(name),Customer::getCustomerName, name)
                .like(StringUtils.hasLength(code),Customer::getCustomerCode, code));
        List<Customer> customers = page.getRecords().stream().sorted(Comparator.comparing(Customer::getCustomerName)).collect(Collectors.toList());
        page.setRecords(customers);
        return page;
    }

}


