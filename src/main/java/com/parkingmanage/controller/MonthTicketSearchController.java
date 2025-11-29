package com.parkingmanage.controller;

import com.parkingmanage.dto.MonthTicketVehicleDTO;
import com.parkingmanage.dto.SearchResult;
import com.parkingmanage.service.MonthTicketSearchService;
import com.parkingmanage.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 月票车辆搜索控制器
 */
@Api(tags = "月票车辆搜索接口")
@RestController
@RequestMapping("/parking/monthTicket")
@CrossOrigin
public class MonthTicketSearchController {

    @Autowired
    private MonthTicketSearchService searchService;

    /**
     * 智能搜索月票车辆
     */
    @ApiOperation("智能搜索月票车辆")
    @GetMapping("/smartSearch")
    public ResponseEntity<Result> smartSearch(
        @ApiParam("搜索关键词") @RequestParam String keyword,
        @ApiParam("车场名称") @RequestParam(required = false) String parkName,
        @ApiParam("是否只返回在场车辆") @RequestParam(defaultValue = "false") Boolean onlyInPark,
        @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
        @ApiParam("每页大小") @RequestParam(defaultValue = "100") Integer size
    ) {
        try {
            SearchResult<MonthTicketVehicleDTO> result = searchService.smartSearch(keyword, parkName, onlyInPark, page, size);
            return ResponseEntity.ok(Result.success(result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("搜索失败: " + e.getMessage()));
        }
    }

    /**
     * 获取车辆详细信息（不含在场状态，快速查询）
     */
    @ApiOperation("获取车辆详细信息")
    @GetMapping("/getVehicleDetails")
    public ResponseEntity<Result> getVehicleDetails(
        @ApiParam("车牌号") @RequestParam String plateNumber
    ) {
        try {
            MonthTicketVehicleDTO details = searchService.getVehicleDetails(plateNumber);
            if (details != null) {
                return ResponseEntity.ok(Result.success(details));
            } else {
                return ResponseEntity.ok(Result.error("未找到该车辆信息"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取车辆详细信息（包含在场状态）
     * 专门用于用户选择具体车牌后的详细查询
     */
    @ApiOperation("获取车辆详细信息（包含在场状态）")
    @GetMapping("/getVehicleDetailsWithParkStatus")
    public ResponseEntity<Result> getVehicleDetailsWithParkStatus(
        @ApiParam("车牌号") @RequestParam String plateNumber,
        @ApiParam("车场代码") @RequestParam(required = false) String parkCode
    ) {
        try {
            MonthTicketVehicleDTO details = searchService.getVehicleDetailsWithParkStatus(plateNumber, parkCode);
            if (details != null) {
                return ResponseEntity.ok(Result.success(details));
            } else {
                return ResponseEntity.ok(Result.error("未找到该车辆信息"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取车牌号建议列表
     */
    @ApiOperation("获取车牌号建议列表")
    @GetMapping("/getPlateSuggestions")
    public ResponseEntity<Result> getPlateSuggestions(
        @ApiParam("搜索关键词") @RequestParam String keyword,
        @ApiParam("车场名称") @RequestParam(required = false) String parkName,
        @ApiParam("限制数量") @RequestParam(defaultValue = "10") Integer limit
    ) {
        try {
            List<MonthTicketVehicleDTO> suggestions = searchService.getPlateSuggestions(keyword, parkName, limit);
            return ResponseEntity.ok(Result.success(suggestions));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("获取建议失败: " + e.getMessage()));
        }
    }

    /**
     * 检查车辆是否在场
     */
    @ApiOperation("检查车辆是否在场")
    @GetMapping("/checkVehicleInPark")
    public ResponseEntity<Result> checkVehicleInPark(
        @ApiParam("车牌号") @RequestParam String plateNumber,
        @ApiParam("车场代码") @RequestParam(required = false) String parkCode
    ) {
        try {
            Boolean isInPark = searchService.checkVehicleInPark(plateNumber, parkCode);
            return ResponseEntity.ok(Result.success(isInPark));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }

    /**
     * 获取车辆统计信息
     */
    @ApiOperation("获取车辆统计信息")
    @GetMapping("/getVehicleStats")
    public ResponseEntity<Result> getVehicleStats(
        @ApiParam("车牌号") @RequestParam String plateNumber
    ) {
        try {
            Integer appointmentCount = searchService.getAppointmentCount(plateNumber);
            Integer violationCount = searchService.getViolationCount(plateNumber);
            
            // 构建统计信息
            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("plateNumber", plateNumber);
            stats.put("appointmentCount", appointmentCount);
            stats.put("violationCount", violationCount);
            stats.put("creditScore", 100 - (violationCount * 5)); // 简单的信用分数计算
            
            return ResponseEntity.ok(Result.success(stats));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(Result.error("查询失败: " + e.getMessage()));
        }
    }
} 