package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.parkingmanage.entity.Whitelist;
import com.parkingmanage.exception.BusinessException;
import com.parkingmanage.service.IWhitelistService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 白名单管理 前端控制器
 * </p>
 *
 * @author MLH
 * @since 2025-10-07
 */
@Slf4j
@Api(tags = "白名单管理接口")
@RestController
@RequestMapping("/parking/whitelist")
public class WhitelistController {

    @Autowired
    private IWhitelistService whitelistService;

    /**
     * 分页查询白名单列表
     */
    @ApiOperation(value = "分页查询白名单列表", notes = "支持车牌号、车主姓名、车主电话、车场名称筛选")
    @GetMapping
    public Map<String, Object> getWhitelistList(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam(value = "车牌号") @RequestParam(required = false) String plateNumber,
            @ApiParam(value = "车主姓名") @RequestParam(required = false) String ownerName,
            @ApiParam(value = "车主电话") @RequestParam(required = false) String ownerPhone,
            @ApiParam(value = "停车场名称") @RequestParam(required = false) String parkName) {
        
        log.info("🔍 [白名单查询] 查询参数: page={}, size={}, plateNumber={}, ownerName={}, ownerPhone={}, parkName={}", 
                 page, size, plateNumber, ownerName, ownerPhone, parkName);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            IPage<Whitelist> pageResult = whitelistService.getWhitelistPage(page, size, plateNumber, 
                                                                              ownerName, ownerPhone, parkName);
            
            response.put("code", "0");
            response.put("msg", "查询成功");
            
            Map<String, Object> data = new HashMap<>();
            data.put("records", pageResult.getRecords());
            data.put("total", pageResult.getTotal());
            data.put("size", pageResult.getSize());
            data.put("current", pageResult.getCurrent());
            data.put("pages", pageResult.getPages());
            
            response.put("data", data);
            
            log.info("✅ [白名单查询] 查询成功，共{}条记录", pageResult.getTotal());
        } catch (Exception e) {
            log.error("❌ [白名单查询] 查询失败", e);
            response.put("code", "1");
            response.put("msg", "查询失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 创建白名单记录
     */
    @ApiOperation(value = "创建白名单记录", notes = "添加新的白名单记录")
    @PostMapping
    public Map<String, Object> createWhitelist(@RequestBody Whitelist whitelist) {
        log.info("🆕 [白名单创建] 创建白名单: plateNumber={}, parkName={}", 
                 whitelist.getPlateNumber(), whitelist.getParkName());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = whitelistService.createWhitelist(whitelist);
            
            if (success) {
                response.put("code", "0");
                response.put("msg", "创建成功");
                response.put("data", whitelist);
                log.info("✅ [白名单创建] 创建成功: id={}", whitelist.getId());
            } else {
                response.put("code", "1");
                response.put("msg", "创建失败");
                log.warn("⚠️ [白名单创建] 创建失败");
            }
        } catch (BusinessException e) {
            // 业务异常，只记录警告信息，不打印堆栈
            log.warn("⚠️ [白名单创建] {}", e.getMessage());
            response.put("code", e.getCode());
            response.put("msg", e.getMessage());
        } catch (Exception e) {
            // 系统异常，记录完整错误信息
            log.error("❌ [白名单创建] 创建失败", e);
            response.put("code", "1");
            response.put("msg", "系统错误: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 更新白名单记录
     */
    @ApiOperation(value = "更新白名单记录", notes = "更新已有的白名单记录")
    @PutMapping("/{id}")
    public Map<String, Object> updateWhitelist(
            @ApiParam(value = "白名单ID") @PathVariable Long id,
            @RequestBody Whitelist whitelist) {
        
        log.info("📝 [白名单更新] 更新白名单: id={}, plateNumber={}", id, whitelist.getPlateNumber());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            whitelist.setId(id);
            boolean success = whitelistService.updateWhitelist(whitelist);
            
            if (success) {
                response.put("code", "0");
                response.put("msg", "更新成功");
                response.put("data", whitelist);
                log.info("✅ [白名单更新] 更新成功: id={}", id);
            } else {
                response.put("code", "1");
                response.put("msg", "更新失败");
                log.warn("⚠️ [白名单更新] 更新失败: id={}", id);
            }
        } catch (BusinessException e) {
            // 业务异常，只记录警告信息，不打印堆栈
            log.warn("⚠️ [白名单更新] {}", e.getMessage());
            response.put("code", e.getCode());
            response.put("msg", e.getMessage());
        } catch (Exception e) {
            // 系统异常，记录完整错误信息
            log.error("❌ [白名单更新] 更新失败", e);
            response.put("code", "1");
            response.put("msg", "系统错误: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 删除白名单记录
     */
    @ApiOperation(value = "删除白名单记录", notes = "删除指定的白名单记录")
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteWhitelist(@ApiParam(value = "白名单ID") @PathVariable Long id) {
        log.info("🗑️ [白名单删除] 删除白名单: id={}", id);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = whitelistService.deleteWhitelist(id);
            
            if (success) {
                response.put("code", "0");
                response.put("msg", "删除成功");
                log.info("✅ [白名单删除] 删除成功: id={}", id);
            } else {
                response.put("code", "1");
                response.put("msg", "删除失败");
                log.warn("⚠️ [白名单删除] 删除失败: id={}", id);
            }
        } catch (Exception e) {
            log.error("❌ [白名单删除] 删除失败", e);
            response.put("code", "1");
            response.put("msg", "删除失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 批量删除白名单记录
     */
    @ApiOperation(value = "批量删除白名单记录", notes = "批量删除多条白名单记录")
    @PostMapping("/batch-delete")
    public Map<String, Object> batchDeleteWhitelist(@RequestBody Map<String, List<Long>> params) {
        List<Long> ids = params.get("ids");
        log.info("🗑️ [白名单批量删除] 批量删除白名单: ids={}", ids);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = whitelistService.batchDeleteWhitelist(ids);
            
            if (success) {
                response.put("code", "0");
                response.put("msg", "批量删除成功");
                log.info("✅ [白名单批量删除] 批量删除成功，共删除{}条记录", ids.size());
            } else {
                response.put("code", "1");
                response.put("msg", "批量删除失败");
                log.warn("⚠️ [白名单批量删除] 批量删除失败");
            }
        } catch (Exception e) {
            log.error("❌ [白名单批量删除] 批量删除失败", e);
            response.put("code", "1");
            response.put("msg", "批量删除失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 根据车牌号查询白名单记录
     */
    @ApiOperation(value = "根据车牌号查询白名单记录", notes = "查询指定车牌和车场的白名单记录")
    @GetMapping("/by-plate")
    public Map<String, Object> getWhitelistByPlate(
            @ApiParam(value = "车牌号", required = true) @RequestParam String plateNumber,
            @ApiParam(value = "停车场名称", required = true) @RequestParam String parkName) {
        
        log.info("🔍 [白名单查询] 根据车牌查询白名单: plateNumber={}, parkName={}", plateNumber, parkName);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Whitelist whitelist = whitelistService.getWhitelistByPlate(plateNumber, parkName);
            
            response.put("code", "0");
            response.put("msg", "查询成功");
            response.put("data", whitelist);
            
            if (whitelist != null) {
                log.info("✅ [白名单查询] 找到白名单记录: id={}", whitelist.getId());
            } else {
                log.info("ℹ️ [白名单查询] 未找到白名单记录");
            }
        } catch (Exception e) {
            log.error("❌ [白名单查询] 查询失败", e);
            response.put("code", "1");
            response.put("msg", "查询失败: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * 检查车牌是否在白名单中
     */
    @ApiOperation(value = "检查车牌是否在白名单中", notes = "验证指定车牌和车场是否在白名单中")
    @GetMapping("/check")
    public Map<String, Object> checkWhitelist(
            @ApiParam(value = "车牌号", required = true) @RequestParam String plateNumber,
            @ApiParam(value = "停车场名称", required = true) @RequestParam String parkName) {
        
        log.info("🔍 [白名单检查] 检查车牌是否在白名单: plateNumber={}, parkName={}", plateNumber, parkName);
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean inWhitelist = whitelistService.checkWhitelist(plateNumber, parkName);
            
            response.put("code", "0");
            response.put("msg", "检查成功");
            response.put("data", inWhitelist);
            
            log.info("✅ [白名单检查] 检查结果: inWhitelist={}", inWhitelist);
        } catch (Exception e) {
            log.error("❌ [白名单检查] 检查失败", e);
            response.put("code", "1");
            response.put("msg", "检查失败: " + e.getMessage());
        }
        
        return response;
    }
}

