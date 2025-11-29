package com.parkingmanage.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Purchase;
import com.parkingmanage.service.PurchaseService;
import com.parkingmanage.utils.PageUtils;
import com.parkingmanage.vo.PurchaseVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 <p>
 采申请管理 前端控制器
 </p>

 @author yuli
 @since 2022-02-28
*/
@RestController
@RequestMapping("/parking/purchase")
public class                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    PurchaseController {
    @Resource
    private PurchaseService purchaseService;

    @ApiOperation("添加")
    @PostMapping
    public ResponseEntity<Result> insertPurchase(@RequestBody Purchase purchase) {
        purchase.setApplicationTime(LocalDateTime.now());
        purchase.setAuditStatus(0);
        purchaseService.save(purchase);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("修改")
    @PutMapping
    public ResponseEntity<Result> update(@RequestBody Purchase purchase) {
        purchase.setAudiusTime(LocalDateTime.now());
        purchaseService.updateById(purchase);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("删除")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purchaseService.removeById(id);
    }

    @ApiOperation("查询单条")
    @GetMapping("/{id}")
    public Purchase findById(@PathVariable String id) {
        return purchaseService.getById(id);
    }

    @ApiOperation("查询所有")
    @GetMapping("/list")
    public List<Purchase> findAll() {
        return purchaseService.list();
    }

    @ApiOperation("分页查询")
    @GetMapping("/page")
    public ResponseEntity<Result> getPageList(
            @RequestParam(required = false, value = "departmentId") Integer departmentId,
            @RequestParam(required = false, value = "deviceName") String deviceName,
            @RequestParam(required = false, value = "applicationTime") String applicationTime,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<PurchaseVo> purchaseVos = purchaseService.queryPurchas(departmentId, deviceName, applicationTime, null, null);
        return ResponseEntity.ok(Result.success(PageUtils.getPage(purchaseVos, pageNum, pageSize)));
    }

    @ApiOperation("导出")
    @GetMapping("/exportPurchaseManagement")
    public void exportPurchaseManagement(HttpServletResponse response) {
        purchaseService.exportPurchaseManagement(response);
    }

    @ApiOperation("审批")
    @GetMapping("/queryexportPurchaseManagement")
    public void exportPurchaseManagement(
            @RequestParam(required = false, value = "departmentId") Integer departmentId,
            @RequestParam(required = false, value = "deviceName") String deviceName,
            @RequestParam(required = false, value = "audiusTime") String audiusTime,
            @RequestParam(required = false, value = "auditStatus") Integer auditStatus,
            @RequestParam(required = false, value = "audiusUserId") Integer audiusUserId,
            HttpServletResponse response) {
        purchaseService.queryExportPurchaseManagement(departmentId, deviceName, audiusTime, auditStatus, audiusUserId, response);
    }

    @ApiOperation("审批分页查询")
    @GetMapping("/queryPage")
    public ResponseEntity<Result> queryPage(
            @RequestParam(required = false, value = "departmentId") Integer departmentId,
            @RequestParam(required = false, value = "deviceName") String deviceName,
            @RequestParam(required = false, value = "audiusTime") String audiusTime,
            @RequestParam(required = false, value = "auditStatus") Integer auditStatus,
            @RequestParam(required = true, value = "audiusUserId") Integer audiusUserId,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<PurchaseVo> purchaseVos = purchaseService.queryPurchas(departmentId, deviceName, null, audiusTime, audiusUserId);
        if (auditStatus != null) {
            purchaseVos = purchaseVos.stream().filter((PurchaseVo purchaseVo) -> auditStatus != null && auditStatus.equals(purchaseVo.getAuditStatus())).collect(Collectors.toList());
        }
        return ResponseEntity.ok(Result.success(PageUtils.getPage(purchaseVos, pageNum, pageSize)));
    }

    @ApiOperation("修改")
    @PostMapping("/updatePurchaseVoucher")
    public ResponseEntity<Result> updatePurchaseVoucher(@RequestParam(value = "files", required = false) MultipartFile[] files,
                                                        @RequestParam(value = "purchaseId") Integer purchaseId) {
        purchaseService.updatePurchaseVoucher(files, purchaseId);
        return ResponseEntity.ok(new Result());
    }

    @ApiOperation("审批分页查询")
    @GetMapping("/queryPagebuy")
    public ResponseEntity<Result> queryPagebuy(
            @RequestParam(required = false, value = "departmentId") Integer departmentId,
            @RequestParam(required = false, value = "deviceName") String deviceName,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        List<PurchaseVo> purchaseVos = purchaseService.queryPurchas(departmentId, deviceName, null, null, null);
        purchaseVos = purchaseVos.stream().filter((PurchaseVo purchaseVo) -> purchaseVo.getAuditStatus()==1 || purchaseVo.getAuditStatus()==3).collect(Collectors.toList());

        return ResponseEntity.ok(Result.success(PageUtils.getPage(purchaseVos, pageNum, pageSize)));
    }

    @ApiOperation("查询所有")
    @GetMapping("/listByType")
    public List<Purchase> getAll() {
        return purchaseService.list(new LambdaQueryWrapper<Purchase>().eq(Purchase ::getAuditStatus,3));
    }
}