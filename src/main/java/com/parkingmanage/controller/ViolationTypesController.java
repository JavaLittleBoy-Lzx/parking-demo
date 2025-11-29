package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.entity.ViolationTypes;
import com.parkingmanage.service.ViolationTypesService;
import com.parkingmanage.service.AIDescriptionSuggestionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 违规类型配置管理 前端控制器
 * </p>
 *
 * @author MLH
 * @since 2025-01-31
 */
@RestController
@RequestMapping("/parking/violation-types")
@Api(tags = "违规类型配置管理")
public class ViolationTypesController {

    @Resource
    private ViolationTypesService violationTypesService;

    @Resource
    private AIDescriptionSuggestionService aiDescriptionSuggestionService;

    @GetMapping
    @ApiOperation("获取违规类型列表")
    public Result<Map<String, List<ViolationTypes>>> getViolationTypes() {
        Map<String, List<ViolationTypes>> result = violationTypesService.getViolationTypesByCategory();
        return Result.success(result);
    }

    @GetMapping("/active")
    @ApiOperation("获取启用的违规类型列表")
    public Result<List<ViolationTypes>> getActiveTypes() {
        List<ViolationTypes> result = violationTypesService.getActiveTypes();
        return Result.success(result);
    }

    @PostMapping
    @ApiOperation("创建违规类型")
    public Result<Boolean> createViolationType(@RequestBody ViolationTypes violationType) {
        boolean result = violationTypesService.createViolationType(violationType);
        return result ? Result.success(true) : Result.error("创建失败，违规类型值已存在");
    }

    @PutMapping("/{id}")
    @ApiOperation("更新违规类型")
    public Result<Boolean> updateViolationType(
            @ApiParam("违规类型ID") @PathVariable Long id,
            @RequestBody ViolationTypes violationType) {
        
        violationType.setId(id);
        boolean result = violationTypesService.updateViolationType(violationType);
        return result ? Result.success(true) : Result.error("更新失败，违规类型值已存在");
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除违规类型")
    public Result<Boolean> deleteViolationType(@ApiParam("违规类型ID") @PathVariable Long id) {
        boolean result = violationTypesService.deleteViolationType(id);
        return result ? Result.success(true) : Result.error("删除失败");
    }

    @PutMapping("/{value}/usage")
    @ApiOperation("更新违规类型使用次数")
    public Result<Boolean> updateUsageCount(@ApiParam("违规类型值") @PathVariable String value) {
        boolean result = violationTypesService.updateUsageCount(value);
        return result ? Result.success(true) : Result.error("更新失败");
    }

    @GetMapping("/check-value")
    @ApiOperation("检查违规类型值是否已存在")
    public Result<Boolean> checkValueExists(
            @ApiParam("违规类型值") @RequestParam String value,
            @ApiParam("排除的ID") @RequestParam(required = false) Long excludeId) {
        
        boolean exists = violationTypesService.checkValueExists(value, excludeId);
        return Result.success(exists);
    }

    @GetMapping("/ai-suggestions")
    @ApiOperation("AI智能生成违规描述建议")
    public Result<List<String>> getAISuggestions(
            @ApiParam("违规类型名称") @RequestParam String typeName) {
        
        List<String> suggestions = aiDescriptionSuggestionService.generateSuggestions(typeName);
        return Result.success(suggestions);
    }
}
