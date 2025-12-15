package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 车场管理控制器
 * 提供车场相关的基础接口
 * 
 * @author parking-system
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/api/park")
@CrossOrigin(origins = "*")
public class ParkController {

    /**
     * 获取车场列表
     * 
     * @return 车场列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getParkList() {
        try {
            // 这里应该从数据库获取真实的车场数据
            // 暂时返回模拟数据
            List<Map<String, Object>> parkList = Arrays.asList(
                createParkInfo(1, "中央商务区停车场", "CBD中心", "正常"),
                createParkInfo(2, "万达广场停车场", "万达购物中心", "正常"),
                createParkInfo(3, "市政府停车场", "市政府大楼", "正常"),
                createParkInfo(4, "火车站停车场", "火车站广场", "正常"),
                createParkInfo(5, "机场停车场", "国际机场", "正常"),
                createParkInfo(6, "体育中心停车场", "体育馆", "正常"),
                createParkInfo(7, "医院停车场", "人民医院", "正常"),
                createParkInfo(8, "大学停车场", "科技大学", "正常")
            );
            
            return Result.success(parkList);
            
        } catch (Exception e) {
            return Result.error("500", "获取车场列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建车场信息对象
     */
    private Map<String, Object> createParkInfo(Integer id, String name, String location, String status) {
        Map<String, Object> park = new HashMap<>();
        park.put("id", id);
        park.put("name", name);
        park.put("location", location);
        park.put("status", status);
        return park;
    }
} 