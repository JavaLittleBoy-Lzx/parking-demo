package com.parkingmanage.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.entity.BlacklistReason;
import com.parkingmanage.entity.ViolationDescription;
import com.parkingmanage.entity.ViolationLocation;
import com.parkingmanage.entity.ViolationType;
import com.parkingmanage.service.BlacklistReasonService;
import com.parkingmanage.service.ViolationDescriptionService;
import com.parkingmanage.service.ViolationLocationService;
import com.parkingmanage.service.ViolationTypeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 违规配置管理Controller
 * @author system
 * @date 2025-01-31
 */
@Slf4j
@Api(tags = "违规配置管理")
@RestController
@RequestMapping("/parking/violation-config")
@CrossOrigin
public class ViolationConfigController {

    @Autowired
    private ViolationLocationService violationLocationService;

    @Autowired
    private ViolationTypeService violationTypeService;

    @Autowired
    private ViolationDescriptionService violationDescriptionService;

    @Autowired
    private BlacklistReasonService blacklistReasonService;

    // ==================== 违规位置管理 ====================

    @ApiOperation("分页查询违规位置列表")
    @GetMapping("/locations")
    public Map<String, Object> getLocationPage(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("每页大小") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("位置名称") @RequestParam(required = false) String locationName,
            @ApiParam("车场名称") @RequestParam(required = false) String parkName,
            @ApiParam("是否启用") @RequestParam(required = false) Boolean isEnabled) {
        
        log.info("🔍 [查询违规位置] page={}, size={}, locationName={}, parkName={}, isEnabled={}", 
            page, size, locationName, parkName, isEnabled);
        
        Page<ViolationLocation> result = violationLocationService.getLocationPage(
            page, size, locationName, parkName, isEnabled);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", "0");
        response.put("msg", "查询成功");
        response.put("data", result);
        return response;
    }

    @ApiOperation("查询启用的违规位置列表（下拉选择）")
    @GetMapping("/locations/enabled")
    public Map<String, Object> getEnabledLocations(
            @ApiParam("车场名称") @RequestParam(required = false) String parkName) {
        
        log.info("🔍 [查询启用的违规位置] parkName={}", parkName);
        
        List<ViolationLocation> result = violationLocationService.getEnabledLocations(parkName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", "0");
        response.put("msg", "查询成功");
        response.put("data", result);
        return response;
    }

    @ApiOperation("新增违规位置")
    @PostMapping("/locations")
    public Map<String, Object> addLocation(@RequestBody ViolationLocation location) {
        log.info("➕ [新增违规位置] location={}", location);
        
        try {
            location.setCreatedAt(LocalDateTime.now());
            location.setUpdatedAt(LocalDateTime.now());
            boolean success = violationLocationService.addLocation(location);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "新增成功" : "新增失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [新增违规位置失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    @ApiOperation("更新违规位置")
    @PutMapping("/locations/{id}")
    public Map<String, Object> updateLocation(
            @ApiParam("位置ID") @PathVariable Long id,
            @RequestBody ViolationLocation location) {
        log.info("📝 [更新违规位置] id={}, location={}", id, location);
        
        try {
            location.setId(id);
            location.setUpdatedAt(LocalDateTime.now());
            boolean success = violationLocationService.updateLocation(location);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "更新成功" : "更新失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [更新违规位置失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    @ApiOperation("删除违规位置")
    @DeleteMapping("/locations/{id}")
    public Map<String, Object> deleteLocation(@ApiParam("位置ID") @PathVariable Long id) {
        log.info("🗑️ [删除违规位置] id={}", id);
        
        try {
            boolean success = violationLocationService.deleteLocation(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "删除成功" : "删除失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [删除违规位置失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    @ApiOperation("切换违规位置启用状态")
    @PatchMapping("/locations/{id}/toggle")
    public Map<String, Object> toggleLocationEnabled(
            @ApiParam("位置ID") @PathVariable Long id,
            @ApiParam("是否启用") @RequestParam Boolean isEnabled) {
        log.info("🔄 [切换违规位置状态] id={}, isEnabled={}", id, isEnabled);
        
        try {
            boolean success = violationLocationService.toggleEnabled(id, isEnabled);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "操作成功" : "操作失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [切换违规位置状态失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    // ==================== 违规类型管理 ====================

    @ApiOperation("分页查询违规类型列表")
    @GetMapping("/types")
    public Map<String, Object> getTypePage(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("每页大小") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("类型名称") @RequestParam(required = false) String typeName,
            @ApiParam("车场名称") @RequestParam(required = false) String parkName,
            @ApiParam("严重程度") @RequestParam(required = false) String severityLevel,
            @ApiParam("是否启用") @RequestParam(required = false) Boolean isEnabled) {
        
        log.info("🔍 [查询违规类型] page={}, size={}, typeName={}, parkName={}, severityLevel={}, isEnabled={}", 
            page, size, typeName, parkName, severityLevel, isEnabled);
        
        Page<ViolationType> result = violationTypeService.getTypePage(
            page, size, typeName, parkName, severityLevel, isEnabled);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", "0");
        response.put("msg", "查询成功");
        response.put("data", result);
        return response;
    }

    @ApiOperation("查询启用的违规类型列表（下拉选择）")
    @GetMapping("/types/enabled")
    public Map<String, Object> getEnabledTypes(
            @ApiParam("车场名称") @RequestParam(required = false) String parkName) {
        
        log.info("🔍 [查询启用的违规类型] parkName={}", parkName);
        
        List<ViolationType> result = violationTypeService.getEnabledTypes(parkName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", "0");
        response.put("msg", "查询成功");
        response.put("data", result);
        return response;
    }

    @ApiOperation("新增违规类型")
    @PostMapping("/types")
    public Map<String, Object> addType(@RequestBody ViolationType type) {
        log.info("➕ [新增违规类型] type={}", type);
        
        try {
            type.setCreatedAt(LocalDateTime.now());
            type.setUpdatedAt(LocalDateTime.now());
            boolean success = violationTypeService.addType(type);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "新增成功" : "新增失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [新增违规类型失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    @ApiOperation("更新违规类型")
    @PutMapping("/types/{id}")
    public Map<String, Object> updateType(
            @ApiParam("类型ID") @PathVariable Long id,
            @RequestBody ViolationType type) {
        log.info("📝 [更新违规类型] id={}, type={}", id, type);
        
        try {
            type.setId(id);
            type.setUpdatedAt(LocalDateTime.now());
            boolean success = violationTypeService.updateType(type);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "更新成功" : "更新失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [更新违规类型失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    @ApiOperation("删除违规类型")
    @DeleteMapping("/types/{id}")
    public Map<String, Object> deleteType(@ApiParam("类型ID") @PathVariable Long id) {
        log.info("🗑️ [删除违规类型] id={}", id);
        
        try {
            boolean success = violationTypeService.deleteType(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "删除成功" : "删除失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [删除违规类型失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    @ApiOperation("切换违规类型启用状态")
    @PatchMapping("/types/{id}/toggle")
    public Map<String, Object> toggleTypeEnabled(
            @ApiParam("类型ID") @PathVariable Long id,
            @ApiParam("是否启用") @RequestParam Boolean isEnabled) {
        log.info("🔄 [切换违规类型状态] id={}, isEnabled={}", id, isEnabled);
        
        try {
            boolean success = violationTypeService.toggleEnabled(id, isEnabled);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "操作成功" : "操作失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [切换违规类型状态失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    // ==================== 违规描述管理 ====================

    @ApiOperation("分页查询违规描述列表")
    @GetMapping("/descriptions")
    public Map<String, Object> getDescriptionPage(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("每页大小") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("描述内容") @RequestParam(required = false) String descriptionText,
            @ApiParam("违规类型代码") @RequestParam(required = false) String violationTypeCode,
            @ApiParam("车场名称") @RequestParam(required = false) String parkName,
            @ApiParam("是否启用") @RequestParam(required = false) Boolean isEnabled) {
        
        log.info("🔍 [查询违规描述] page={}, size={}, descriptionText={}, violationTypeCode={}, parkName={}, isEnabled={}", 
            page, size, descriptionText, violationTypeCode, parkName, isEnabled);
        
        Page<ViolationDescription> result = violationDescriptionService.getDescriptionPage(
            page, size, descriptionText, violationTypeCode, parkName, isEnabled);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", "0");
        response.put("msg", "查询成功");
        response.put("data", result);
        return response;
    }

    @ApiOperation("查询启用的违规描述列表（下拉选择）")
    @GetMapping("/descriptions/enabled")
    public Map<String, Object> getEnabledDescriptions(
            @ApiParam("违规类型代码") @RequestParam(required = false) String violationTypeCode,
            @ApiParam("车场名称") @RequestParam(required = false) String parkName) {
        
        log.info("🔍 [查询启用的违规描述] violationTypeCode={}, parkName={}", violationTypeCode, parkName);
        
        List<ViolationDescription> result = violationDescriptionService.getEnabledDescriptions(
            violationTypeCode, parkName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", "0");
        response.put("msg", "查询成功");
        response.put("data", result);
        return response;
    }

    @ApiOperation("新增违规描述")
    @PostMapping("/descriptions")
    public Map<String, Object> addDescription(@RequestBody ViolationDescription description) {
        log.info("➕ [新增违规描述] description={}", description);
        
        try {
            description.setCreatedAt(LocalDateTime.now());
            description.setUpdatedAt(LocalDateTime.now());
            boolean success = violationDescriptionService.addDescription(description);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "新增成功" : "新增失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [新增违规描述失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    @ApiOperation("更新违规描述")
    @PutMapping("/descriptions/{id}")
    public Map<String, Object> updateDescription(
            @ApiParam("描述ID") @PathVariable Long id,
            @RequestBody ViolationDescription description) {
        log.info("📝 [更新违规描述] id={}, description={}", id, description);
        
        try {
            description.setId(id);
            description.setUpdatedAt(LocalDateTime.now());
            boolean success = violationDescriptionService.updateDescription(description);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "更新成功" : "更新失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [更新违规描述失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    @ApiOperation("删除违规描述")
    @DeleteMapping("/descriptions/{id}")
    public Map<String, Object> deleteDescription(@ApiParam("描述ID") @PathVariable Long id) {
        log.info("🗑️ [删除违规描述] id={}", id);
        
        try {
            boolean success = violationDescriptionService.deleteDescription(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "删除成功" : "删除失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [删除违规描述失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    @ApiOperation("切换违规描述启用状态")
    @PatchMapping("/descriptions/{id}/toggle")
    public Map<String, Object> toggleDescriptionEnabled(
            @ApiParam("描述ID") @PathVariable Long id,
            @ApiParam("是否启用") @RequestParam Boolean isEnabled) {
        log.info("🔄 [切换违规描述状态] id={}, isEnabled={}", id, isEnabled);
        
        try {
            boolean success = violationDescriptionService.toggleEnabled(id, isEnabled);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "操作成功" : "操作失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [切换违规描述状态失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    // ==================== 拉黑原因管理 ====================

    @ApiOperation("分页查询拉黑原因列表")
    @GetMapping("/reasons")
    public Map<String, Object> getReasonPage(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("每页大小") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("原因内容") @RequestParam(required = false) String reasonText,
            @ApiParam("原因分类") @RequestParam(required = false) String reasonCategory,
            @ApiParam("车场名称") @RequestParam(required = false) String parkName,
            @ApiParam("是否启用") @RequestParam(required = false) Boolean isEnabled) {
        
        log.info("🔍 [查询拉黑原因] page={}, size={}, reasonText={}, reasonCategory={}, parkName={}, isEnabled={}", 
            page, size, reasonText, reasonCategory, parkName, isEnabled);
        
        Page<BlacklistReason> result = blacklistReasonService.getReasonPage(
            page, size, reasonText, reasonCategory, parkName, isEnabled);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", "0");
        response.put("msg", "查询成功");
        response.put("data", result);
        return response;
    }

    @ApiOperation("查询启用的拉黑原因列表（下拉选择）")
    @GetMapping("/reasons/enabled")
    public Map<String, Object> getEnabledReasons(
            @ApiParam("原因分类") @RequestParam(required = false) String reasonCategory,
            @ApiParam("车场名称") @RequestParam(required = false) String parkName) {
        
        log.info("🔍 [查询启用的拉黑原因] reasonCategory={}, parkName={}", reasonCategory, parkName);
        
        List<BlacklistReason> result = blacklistReasonService.getEnabledReasons(
            reasonCategory, parkName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", "0");
        response.put("msg", "查询成功");
        response.put("data", result);
        return response;
    }

    @ApiOperation("新增拉黑原因")
    @PostMapping("/reasons")
    public Map<String, Object> addReason(@RequestBody BlacklistReason reason) {
        log.info("➕ [新增拉黑原因] reason={}", reason);
        
        try {
            reason.setCreatedAt(LocalDateTime.now());
            reason.setUpdatedAt(LocalDateTime.now());
            boolean success = blacklistReasonService.addReason(reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "新增成功" : "新增失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [新增拉黑原因失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    @ApiOperation("更新拉黑原因")
    @PutMapping("/reasons/{id}")
    public Map<String, Object> updateReason(
            @ApiParam("原因ID") @PathVariable Long id,
            @RequestBody BlacklistReason reason) {
        log.info("📝 [更新拉黑原因] id={}, reason={}", id, reason);
        
        try {
            reason.setId(id);
            reason.setUpdatedAt(LocalDateTime.now());
            boolean success = blacklistReasonService.updateReason(reason);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "更新成功" : "更新失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [更新拉黑原因失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    @ApiOperation("删除拉黑原因")
    @DeleteMapping("/reasons/{id}")
    public Map<String, Object> deleteReason(@ApiParam("原因ID") @PathVariable Long id) {
        log.info("🗑️ [删除拉黑原因] id={}", id);
        
        try {
            boolean success = blacklistReasonService.deleteReason(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "删除成功" : "删除失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [删除拉黑原因失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }

    @ApiOperation("切换拉黑原因启用状态")
    @PatchMapping("/reasons/{id}/toggle")
    public Map<String, Object> toggleReasonEnabled(
            @ApiParam("原因ID") @PathVariable Long id,
            @ApiParam("是否启用") @RequestParam Boolean isEnabled) {
        log.info("🔄 [切换拉黑原因状态] id={}, isEnabled={}", id, isEnabled);
        
        try {
            boolean success = blacklistReasonService.toggleEnabled(id, isEnabled);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", success ? "0" : "1");
            response.put("msg", success ? "操作成功" : "操作失败");
            return response;
        } catch (Exception e) {
            log.error("❌ [切换拉黑原因状态失败]", e);
            Map<String, Object> response = new HashMap<>();
            response.put("code", "1");
            response.put("msg", e.getMessage());
            return response;
        }
    }
}

