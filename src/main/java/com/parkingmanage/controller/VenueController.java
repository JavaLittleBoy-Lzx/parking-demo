package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.entity.YardInfo;
import com.parkingmanage.service.VehicleReservationService;
import com.parkingmanage.service.YardInfoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 车场（入场点）管理控制器
 * 
 * @author system
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/parking/venue")
@CrossOrigin
@Api(tags = "车场管理")
public class VenueController {
    
    @Resource
    private VehicleReservationService vehicleReservationService;
    
    @Resource
    private YardInfoService yardInfoService;
    
    /**
     * 获取所有车场列表
     * 从yard_info表中获取所有不重复的车场名称（yardName字段）
     * 用于前端用户管理页面的车场选择下拉框
     * 
     * @return 车场名称列表
     */
    @ApiOperation("获取所有车场列表")
    @GetMapping("/listAll")
    public ResponseEntity<Result> listAll() {
        try {
            log.info("🔍 开始从yard_info表查询车场列表");
            
            // 从yard_info表获取所有车场信息
            List<YardInfo> yardInfoList = yardInfoService.yardNameList();
            
            // 提取车场名称，过滤已删除的记录，去重并排序
            List<String> yardNames = yardInfoList.stream()
                    .filter(yard -> yard.getDeleted() == null || yard.getDeleted() == 0) // 过滤未删除的记录
                    .map(YardInfo::getYardName) // 提取车场名称
                    .filter(name -> name != null && !name.trim().isEmpty()) // 过滤空值
                    .distinct() // 去重
                    .sorted() // 排序
                    .collect(Collectors.toList());
            
            log.info("✅ 从yard_info表获取车场列表成功，共{}个车场", yardNames.size());
            if (!yardNames.isEmpty()) {
                log.info("📋 车场列表: {}", yardNames);
            } else {
                log.warn("⚠️ yard_info表中没有有效的车场数据");
            }
            
            return ResponseEntity.ok(Result.success(yardNames));
        } catch (Exception e) {
            log.error("❌ 从yard_info表获取车场列表失败", e);
            return ResponseEntity.ok(Result.error("获取车场列表失败: " + e.getMessage()));
        }
    }
}

