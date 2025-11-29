package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.TestVisitorReservation;
import com.parkingmanage.mapper.TestVisitorReservationMapper;
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
 * 测试用访客预约记录控制器
 * 模拟外部接口，用于测试访客VIP自动开通功能
 * 
 * @author System
 */
@Slf4j
@RestController
@RequestMapping("/parking/nefuData")
@Api(tags = "测试用访客预约接口")
public class TestVisitorReservationController {

    @Autowired
    private TestVisitorReservationMapper reservationMapper;

    /**
     * 分页查询预约记录
     * 
     * @param startTime 开始时间 (格式: yyyy-MM-dd HH:mm:ss)
     * @param endTime 结束时间 (格式: yyyy-MM-dd HH:mm:ss)
     * @param pageNum 页码 (从1开始)
     * @param pageSize 每页大小
     * @return 分页结果
     */
    @PostMapping("/page")
    @ApiOperation("分页查询预约记录（按创建时间范围）")
    public Result<Map<String, Object>> getReservationPage(
            @ApiParam("开始时间") @RequestParam(required = false) String startTime,
            @ApiParam("结束时间") @RequestParam(required = false) String endTime,
            @ApiParam("页码（从1开始）") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam("每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {

        try {
            log.info("📥 [测试接口] 接收到查询请求 - startTime: {}, endTime: {}, pageNum: {}, pageSize: {}", 
                    startTime, endTime, pageNum, pageSize);

            // 参数校验
            if (pageNum < 1) {
                pageNum = 1;
            }
            if (pageSize < 1 || pageSize > 1000) {
                pageSize = 10;
            }

            // 计算分页偏移量（MyBatis的LIMIT需要从0开始）
            int offset = (pageNum - 1) * pageSize;

            // 查询数据
            List<TestVisitorReservation> records = reservationMapper.selectByCreateTimeRange(
                    startTime, endTime, offset, pageSize);

            // 查询总数
            int total = reservationMapper.countByCreateTimeRange(startTime, endTime);

            // 构建返回结果
            Map<String, Object> data = new HashMap<>();
            data.put("records", records);
            data.put("total", total);
            data.put("size", pageSize);
            data.put("current", pageNum);
            data.put("pages", (int) Math.ceil((double) total / pageSize));

            log.info("✅ [测试接口] 查询成功 - 返回 {} 条记录，总计 {} 条", records.size(), total);

            return Result.success(data);

        } catch (Exception e) {
            log.error("❌ [测试接口] 查询失败: {}", e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 查询所有预约记录（不分页）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 所有符合条件的记录
     */
    @GetMapping("/list")
    @ApiOperation("查询所有预约记录（不分页）")
    public Result<List<TestVisitorReservation>> getReservationList(
            @ApiParam("开始时间") @RequestParam(required = false) String startTime,
            @ApiParam("结束时间") @RequestParam(required = false) String endTime) {

        try {
            log.info("📥 [测试接口] 接收到列表查询请求 - startTime: {}, endTime: {}", startTime, endTime);

            QueryWrapper<TestVisitorReservation> wrapper = new QueryWrapper<>();
            
            if (startTime != null && !startTime.isEmpty()) {
                wrapper.ge("ct_date", startTime);
            }
            if (endTime != null && !endTime.isEmpty()) {
                wrapper.le("ct_date", endTime);
            }
            
            wrapper.orderByDesc("ct_date");

            List<TestVisitorReservation> records = reservationMapper.selectList(wrapper);

            log.info("✅ [测试接口] 查询成功 - 返回 {} 条记录", records.size());

            return Result.success(records);

        } catch (Exception e) {
            log.error("❌ [测试接口] 查询失败: {}", e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 添加测试预约记录
     * 
     * @param reservation 预约记录
     * @return 添加结果
     */
    @PostMapping("/add")
    @ApiOperation("添加测试预约记录")
    public Result<String> addReservation(@RequestBody TestVisitorReservation reservation) {
        try {
            log.info("📥 [测试接口] 添加预约记录 - id: {}, visitorUserName: {}", 
                    reservation.getId(), reservation.getVisitorUserName());

            int result = reservationMapper.insert(reservation);

            if (result > 0) {
                log.info("✅ [测试接口] 添加成功");
                return Result.success("添加成功");
            } else {
                log.warn("⚠️ [测试接口] 添加失败");
                return Result.error("添加失败");
            }

        } catch (Exception e) {
            log.error("❌ [测试接口] 添加失败: {}", e.getMessage(), e);
            return Result.error("添加失败: " + e.getMessage());
        }
    }

    /**
     * 批量添加测试预约记录
     * 
     * @param count 生成数量
     * @return 添加结果
     */
    @PostMapping("/generateTestData")
    @ApiOperation("批量生成测试数据")
    public Result<String> generateTestData(@ApiParam("生成数量") @RequestParam(defaultValue = "10") Integer count) {
        try {
            log.info("📥 [测试接口] 生成测试数据 - 数量: {}", count);

            if (count > 100) {
                return Result.error("单次生成数量不能超过100条");
            }

            int successCount = 0;
            for (int i = 0; i < count; i++) {
                TestVisitorReservation reservation = new TestVisitorReservation();
                reservation.setUserId(6886261L + i);
                reservation.setGatewayTransitBeginTime("2025-11-08 15:23:47");
                reservation.setGatewayTransitEndTime("2025-11-08 23:59:59");
                reservation.setBeginTime("2025-11-08 15:23:47");
                reservation.setEndTime("2025-11-08 23:59:59");
                reservation.setApplyFromName("移动端");
                reservation.setApplyFrom(1);
                reservation.setFormId(100023L);
                reservation.setFormName("体育馆自助申请");
                reservation.setVisitorIdCard("230102198304160" + String.format("%03d", i % 1000));
                reservation.setVisitorUserName("测试访客" + i);
                reservation.setVisitorPhoneNo("133599953" + String.format("%02d", i % 100));
                reservation.setPassName(null);
                reservation.setApplyState(0);
                reservation.setApplyStateName("待来访");
                reservation.setUseStatusId(1);
                reservation.setPhoneNo(null);
                reservation.setPassNo(null);
                reservation.setPassDep(null);
                reservation.setCompanionsNum(null);
                reservation.setCodeStr(null);
                reservation.setForeignUserNo(null);
                reservation.setAuthState(1);
                reservation.setAuthStateStr(null);
                reservation.setApprovalFlowId("22347");
                reservation.setSubmitId(21773L + i);
                reservation.setTaskId(null);
                reservation.setVisitorPlateNumber("黑AT" + String.format("%04d", i));
                reservation.setCustomVipName("体育馆自助访客");
                reservation.setBz("自动生成的测试数据");
                reservation.setBz2("");
                reservation.setBz3("");
                reservation.setCtDate("2025-11-08 15:24:11");
                reservation.setOrderByFieldMap(null);

                int result = reservationMapper.insert(reservation);
                if (result > 0) {
                    successCount++;
                }

                // 避免ID重复，睡眠1毫秒
                Thread.sleep(1);
            }

            log.info("✅ [测试接口] 生成完成 - 成功: {} / {}", successCount, count);
            return Result.success("成功生成 " + successCount + " 条测试数据");

        } catch (Exception e) {
            log.error("❌ [测试接口] 生成失败: {}", e.getMessage(), e);
            return Result.error("生成失败: " + e.getMessage());
        }
    }

    /**
     * 删除所有测试数据
     * 
     * @return 删除结果
     */
    @DeleteMapping("/clearTestData")
    @ApiOperation("清空所有测试数据")
    public Result<String> clearTestData() {
        try {
            log.warn("⚠️ [测试接口] 清空所有测试数据");

            QueryWrapper<TestVisitorReservation> wrapper = new QueryWrapper<>();
            int result = reservationMapper.delete(wrapper);

            log.info("✅ [测试接口] 清空完成 - 删除 {} 条记录", result);
            return Result.success("成功删除 " + result + " 条记录");

        } catch (Exception e) {
            log.error("❌ [测试接口] 清空失败: {}", e.getMessage(), e);
            return Result.error("清空失败: " + e.getMessage());
        }
    }

    /**
     * 查询统计信息
     * 
     * @return 统计结果
     */
    @GetMapping("/statistics")
    @ApiOperation("查询统计信息")
    public Result<Map<String, Object>> getStatistics() {
        try {
            QueryWrapper<TestVisitorReservation> wrapper = new QueryWrapper<>();
            int total = reservationMapper.selectCount(wrapper);

            // 按表单名称统计
            QueryWrapper<TestVisitorReservation> wrapper2 = new QueryWrapper<>();
            wrapper2.select("form_name, count(*) as count")
                    .groupBy("form_name");
            List<Map<String, Object>> typeStats = reservationMapper.selectMaps(wrapper2);

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", total);
            stats.put("typeStatistics", typeStats);

            return Result.success(stats);

        } catch (Exception e) {
            log.error("❌ [测试接口] 统计失败: {}", e.getMessage(), e);
            return Result.error("统计失败: " + e.getMessage());
        }
    }
}

