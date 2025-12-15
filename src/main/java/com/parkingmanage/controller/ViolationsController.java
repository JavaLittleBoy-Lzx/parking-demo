package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.common.Result;
import com.parkingmanage.entity.User;
import com.parkingmanage.entity.Violations;
import com.parkingmanage.mapper.ViolationsMapper;
import com.parkingmanage.service.MonthlyTicketTimeoutConfigService;
import com.parkingmanage.service.UserService;
import com.parkingmanage.service.ViolationsService;
import com.parkingmanage.service.WeChatTemplateMessageService;
import com.parkingmanage.utils.TokenUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 违规记录管理 前端控制器
 * </p>
 *
 * @author MLH
 * @since 2025-01-31
 */
@Slf4j
@RestController
@RequestMapping("/parking/violations")
@Api(tags = "违规记录管理")
public class ViolationsController {

    @Resource
    private ViolationsService violationsService;

    @Resource
    private UserService userService;

    @Resource
    private WeChatTemplateMessageService weChatTemplateMessageService;

    @Resource
    private MonthlyTicketTimeoutConfigService monthlyTicketTimeoutConfigService;

    @Resource
    private com.parkingmanage.mapper.UserMappingMapper userMappingMapper;

    @Resource
    private com.parkingmanage.service.impl.ViolationProcessServiceImpl violationProcessService;

    @Resource
    private ViolationsMapper violationsMapper;

    @Resource
    private com.parkingmanage.service.ActivityLogService activityLogService;
    
    @Resource
    private com.parkingmanage.service.AppointmentService appointmentService;

    // ==================== 🔧 工具方法 ====================

    /**
     * 解析黑名单类型：从 "code|name" 格式中提取 name
     * 例如：输入 "31|违章黑名单" 返回 "违章黑名单"
     * 如果没有 "|" 分隔符，则返回原字符串
     */
    private String parseBlacklistTypeName(String blacklistType) {
        if (blacklistType == null || blacklistType.trim().isEmpty()) {
            return blacklistType;
        }

        // 如果包含 "|" 分隔符，提取后面的名称部分
        if (blacklistType.contains("|")) {
            String[] parts = blacklistType.split("\\|", 2);
            if (parts.length == 2) {
                String name = parts[1].trim();
                log.debug("🔧 [解析黑名单类型] 输入: {}, 解析后: {}", blacklistType, name);
                return name;
            }
        }

        // 如果没有分隔符或解析失败，返回原字符串
        return blacklistType.trim();
    }

    @PostMapping
    @ApiOperation("创建违规记录")
    public Result<Map<String, Object>> createViolation(@RequestBody Violations violation, HttpServletRequest request) {
        log.info("🆕 [接收创建违规记录请求] plateNumber={}, appointmentId={}, ownerId={}, createdBy={}, violationType={}",
                violation.getPlateNumber(), violation.getAppointmentId(), violation.getOwnerId(), violation.getCreatedBy(),
                violation.getViolationType());

        // 🔍 添加时间字段调试日志
        log.info("⏰ [时间字段调试] enterTime={}, leaveTime={}, appointmentTime={}",
                violation.getEnterTime(), violation.getLeaveTime(), violation.getAppointmentTime());

        // 验证前端传递的创建者信息
        if (violation.getCreatedBy() == null || violation.getCreatedBy().trim().isEmpty()) {
            log.warn("⚠️ [创建者信息为空] 请求被拒绝");
            return Result.error("创建者信息不能为空");
        }
        // 🆕 记录appointmentId信息
        if (violation.getAppointmentId() != null) {
            log.info("📅 [预约车违规] 接收到appointmentId: {}", violation.getAppointmentId());
        } else {
            log.info("🚗 [非预约车违规] 无appointmentId，将查询本地车主信息");
        }
        
        // 🔒 防重复提交检测：检查60秒内是否有相同车牌号的违规记录
        if (violation.getPlateNumber() != null && !violation.getPlateNumber().trim().isEmpty()) {
            boolean isDuplicate = violationsService.checkDuplicateViolation(
                violation.getPlateNumber(), 
                violation.getViolationType(),
                60 // 60秒内的重复检测
            );
            if (isDuplicate) {
                log.warn("⚠️ [重复提交检测] 60秒内已存在相同车牌号和违规类型的记录 - 车牌: {}, 类型: {}", 
                    violation.getPlateNumber(), violation.getViolationType());
                return Result.error("请勿重复提交，该车辆刚刚已添加过相同类型的违规记录");
            }
        }
        
        boolean result = violationsService.createViolation(violation);
        if (result) {
            log.info("✅ [违规记录创建成功] plateNumber={}, appointmentId={}",
                    violation.getPlateNumber(), violation.getAppointmentId());
            
            // 📝 记录操作日志
            User currentUser = getCurrentUser(request);
            String username = currentUser != null && currentUser.getLoginName() != null 
                            ? currentUser.getLoginName() 
                            : (currentUser != null && currentUser.getUserName() != null 
                                ? currentUser.getUserName() 
                                : violation.getCreatedBy());
            String description = String.format("用户 %s 创建了违规记录：车牌号 %s，违规类型 %s", 
                                              username, 
                                              violation.getPlateNumber(), 
                                              violation.getViolationType() != null ? violation.getViolationType() : "未指定");
            recordOperation(request, "违规管理", "新增违规记录", description);
            
            // 🔔 创建成功后发送微信通知
            Map<String, Object> notifyResult = sendViolationNotification(violation);
            
            // 构建详细的返回数据
            Map<String, Object> responseData = new HashMap<>();
            
            // 违规记录基本信息
            responseData.put("violationCreated", true);
            responseData.put("id", violation.getId());
            responseData.put("plateNumber", violation.getPlateNumber());
            responseData.put("violationType", violation.getViolationType());
            responseData.put("parkName", violation.getParkName());
            responseData.put("location", violation.getLocation());
            responseData.put("status", violation.getStatus());
            
            // 时间信息
            if (violation.getEnterTime() != null) {
                responseData.put("enterTime", violation.getEnterTime().toString());
            }
            if (violation.getCreatedAt() != null) {
                responseData.put("createdAt", violation.getCreatedAt().toString());
            }
            
            // 关联信息
            if (violation.getAppointmentId() != null) {
                responseData.put("appointmentId", violation.getAppointmentId());
            }
            if (violation.getOwnerId() != null) {
                responseData.put("ownerId", violation.getOwnerId());
            }
            
            // 微信通知状态信息
            Map<String, Object> wechatNotifyStatus = new HashMap<>();
            if (notifyResult != null) {
                wechatNotifyStatus.put("required", true);
                wechatNotifyStatus.put("success", notifyResult.get("success"));
                wechatNotifyStatus.put("message", notifyResult.get("message"));
                
                if (!Boolean.TRUE.equals(notifyResult.get("success"))) {
                    String wechatMessage = (String) notifyResult.get("message");
                    responseData.put("wechatNotifyFailed", true);
                    responseData.put("wechatNotifyMessage", wechatMessage);
                    log.warn("⚠️ [违规记录创建成功但微信通知失败] plateNumber={}, 原因: {}", 
                        violation.getPlateNumber(), wechatMessage);
                }
            } else {
                wechatNotifyStatus.put("required", false);
                wechatNotifyStatus.put("reason", "无需发送微信通知");
            }
            responseData.put("wechatNotifyStatus", wechatNotifyStatus);
            
            return Result.success(responseData);
        } else {
            log.error("❌ [违规记录创建失败] plateNumber={}, appointmentId={}",
                    violation.getPlateNumber(), violation.getAppointmentId());
            return Result.error("创建失败");
        }
    }

    @GetMapping
    @ApiOperation("分页查询违规记录")
    public Result<IPage<Map<String, Object>>> getViolations(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("页大小") @RequestParam(defaultValue = "20") Integer size,
            @ApiParam("车牌号") @RequestParam(required = false) String plateNumber,
            @ApiParam("状态") @RequestParam(required = false) String status,
            @ApiParam("违规类型") @RequestParam(required = false) String violationType,
            @ApiParam("开始时间") @RequestParam(required = false) String startDate,
            @ApiParam("结束时间") @RequestParam(required = false) String endDate,
            @ApiParam("车主ID") @RequestParam(required = false) Integer ownerId,
            @ApiParam("用户角色") @RequestParam(required = false) String userRole,
            @ApiParam("小区名称") @RequestParam(required = false) String community,
            @ApiParam("是否使用直接查询") @RequestParam(required = false) Boolean useDirectQuery,
            @ApiParam("创建者") @RequestParam(required = false) String created_by,
            @ApiParam("处理状态") @RequestParam(required = false) String processStatus,
            @ApiParam("处理方式") @RequestParam(required = false) String processType,
            HttpServletRequest request) {
        System.out.println("community = " + community);
        // 解析日期参数，支持多种格式
        LocalDateTime parsedStartDate = parseDateTime(startDate, true);
        LocalDateTime parsedEndDate = parseDateTime(endDate, false);

        // 获取当前用户ID
        String currentUserId = getCurrentUserId(request);
//        System.out.println("")
        System.out.println("currentUserId = " + currentUserId);

        // 🔧 直接使用前端传递的角色参数
        System.out.println("userRole = " + userRole);

        // 根据用户角色决定是否添加创建者过滤条件
        String createdByFilter = null;
        String communityFilter = null;

        if ("patrol".equals(userRole) || "resident".equals(userRole)) {
            // 普通用户只能查看自己创建的记录
            createdByFilter = currentUserId;
        } else if ("housekeeper".equals(userRole) || "manager".equals(userRole) || userRole == null) {
            // 🆕 优先使用前端传递的created_by参数（用于过滤当前用户创建的记录）
            if (created_by != null && !created_by.trim().isEmpty()) {
                createdByFilter = created_by.trim();
                log.info("👤 [管家/管理员] 使用前端传递的创建者参数: {}", createdByFilter);
            }

            // 🆕 优先使用前端传递的park_name参数
            if (community != null && !community.trim().isEmpty()) {
                communityFilter = community.trim();
                System.out.println("使用前端传递的小区参数: " + communityFilter);

                // 🎓 东北林业大学特殊处理：直接查询violations表，不关联其他表
                if (communityFilter.contains("东北林业大学")) {
//                    System.out.println("communityFilter = " + communityFilter);
                    useDirectQuery = true;
                    log.info("🎓 [东北林业大学] 启用直接查询模式，不关联月票表等外部表");
                }
            } else {
                // 如果前端没有传递park_name，则查询用户所在小区
                communityFilter = getCurrentUserCommunity(currentUserId, userRole);
                System.out.println("查询到的用户所在小区: " + communityFilter);
            }
        }

        Page<Map<String, Object>> pageParam = new Page<>(page, size);

        // 🆕 使用新的查询方法，支持处理状态和处理方式筛选
        IPage<Map<String, Object>> result = violationsService.getViolationsWithProcess(
                pageParam, plateNumber, status, violationType, parsedStartDate, parsedEndDate,
                createdByFilter, communityFilter, processStatus, processType, null);

        // System.out.println("result测试数据 = " + result.getTotal());
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    @ApiOperation("更新违规记录状态")
    public Result<Boolean> updateViolationStatus(
            @ApiParam("违规记录ID") @PathVariable Long id,
            @ApiParam("状态") @RequestParam String status,
            @ApiParam("处理备注") @RequestParam(required = false) String remark,
            @ApiParam("处理人ID") @RequestParam(required = false) Integer handlerId,
            @ApiParam("用户角色") @RequestParam(required = false) String userRole,
            HttpServletRequest request) {

        // 获取当前用户ID
        String currentUserId = getCurrentUserId(request);

        // 检查权限：只有管理员、管家或记录创建者可以更新状态
        if (!violationsService.canUpdateViolation(id, currentUserId, userRole)) {
            return Result.error("无权限操作此记录");
        }

        boolean result = violationsService.updateViolationStatus(id, status, remark, handlerId);
        return result ? Result.success(true) : Result.error("更新失败");
    }

    @DeleteMapping("/delete")
    @ApiOperation("删除违规记录")
    public Result<Boolean> deleteViolation(
            @RequestBody Map<String, Object> deleteParams,
            HttpServletRequest request) {
        log.info("🗑️ [接收删除违规记录请求] 参数: {}", deleteParams);
        try {
            // 获取当前用户ID
            String currentUserId = getCurrentUserId(request);
            String userRole = (String) deleteParams.get("userRole");
            // 从请求参数中获取违规记录ID（支持多种字段名）
            Long violationId = null;
            if (deleteParams.containsKey("id")) {
//                Object idValue = deleteParams
                Object idValue = deleteParams.get("id");
                violationId = convertToLong(idValue);
            } else if (deleteParams.containsKey("violationId")) {
                Object idValue = deleteParams.get("violationId");
                violationId = convertToLong(idValue);
            } else if (deleteParams.containsKey("vid")) {
                Object idValue = deleteParams.get("vid");
                violationId = convertToLong(idValue);
            }

            if (violationId == null) {
                log.warn("⚠️ [删除请求被拒绝] 违规记录ID不能为空");
                return Result.error("违规记录ID不能为空");
            }

            log.info("🔍 [删除权限检查] violationId={}, currentUserId={}, userRole={}",
                    violationId, currentUserId, userRole);
            // 检查权限，只有管家管理员或者记录创建者的可以删除violations，CurrentUserId，updateRole
            // 检查权限：只有管理员、管家或记录创建者可以删除
            if (!violationsService.canUpdateViolation(violationId, currentUserId, userRole)) {
                log.warn("⚠️ [删除权限不足] violationId={}, currentUserId={}, userRole={}",
                        violationId, currentUserId, userRole);
                return Result.error("无权限删除此记录");
            }
            // 在删除前获取违规记录信息（用于日志）
            Violations violation = violationsService.getById(violationId);
            String plateNumber = violation != null ? violation.getPlateNumber() : "未知";
            String violationType = violation != null ? violation.getViolationType() : "未知";
            
            // 执行删除操作
            boolean result = violationsService.deleteViolation(violationId, currentUserId);
            if (result) {
                log.info("✅ [违规记录删除成功] violationId={}, deletedBy={}", violationId, currentUserId);
                
                // 📝 记录操作日志
                User currentUser = getCurrentUser(request);
                String username = currentUser != null && currentUser.getLoginName() != null 
                                ? currentUser.getLoginName() 
                                : (currentUser != null && currentUser.getUserName() != null 
                                    ? currentUser.getUserName() 
                                    : currentUserId);
                String description = String.format("用户 %s 删除了违规记录：车牌号 %s，违规类型 %s", 
                                                  username, plateNumber, violationType);
                recordOperation(request, "违规管理", "删除违规记录", description);
                
                return Result.success(true);
            } else {
                log.error("❌ [违规记录删除失败] violationId={}, deletedBy={}", violationId, currentUserId);
                return Result.error("删除失败，记录可能不存在或已被删除");
            }
        } catch (Exception e) {
            log.error("❌ [删除违规记录异常] 参数: {}, 错误: {}", deleteParams, e.getMessage(), e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 🔧 将对象转换为Long类型（支持String、Integer、Long等类型）
     */
    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long) {
            return (Long) value;
        }

        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("⚠️ ID格式转换失败: {}", value);
                return null;
            }
        }

        log.warn("⚠️ 不支持的ID类型: {}", value.getClass().getSimpleName());
        return null;
    }

    @GetMapping("/statistics")
    @ApiOperation("获取违规统计数据")
    public Result<Map<String, Object>> getStatistics(
            @ApiParam("开始时间") @RequestParam(required = false) String startDate,
            @ApiParam("结束时间") @RequestParam(required = false) String endDate,
            @ApiParam("车牌号") @RequestParam(required = false) String plateNumber,
            @ApiParam("用户角色") @RequestParam(required = false) String userRole,
            @ApiParam("小区名称") @RequestParam(required = false) String park_name,
            @ApiParam("创建者") @RequestParam(required = false) String created_by,
            HttpServletRequest request) {

        // 解析日期参数
        LocalDateTime parsedStartDate = parseDateTime(startDate, true);
        LocalDateTime parsedEndDate = parseDateTime(endDate, false);

        // 获取当前用户ID
        String currentUserId = getCurrentUserId(request);

        // 根据用户角色决定统计范围
        String createdByFilter = null;
        String communityFilter = null;

        if ("patrol".equals(userRole) || "resident".equals(userRole)) {
            // 普通用户只能查看自己创建的记录统计
            createdByFilter = currentUserId;
        } else if ("housekeeper".equals(userRole) || "manager".equals(userRole)) {
            // 🆕 优先使用前端传递的created_by参数（用于过滤当前用户创建的记录）
            if (created_by != null && !created_by.trim().isEmpty()) {
                createdByFilter = created_by.trim();
                log.info("👤 [统计接口-管家/管理员] 使用前端传递的创建者参数: {}", createdByFilter);
            }

            // 管家和管理员需要根据小区过滤数据
            if (park_name != null && !park_name.trim().isEmpty()) {
                communityFilter = park_name.trim();
                log.info("🏘️ [统计接口-管家/管理员] 使用前端传递的小区参数: {}", communityFilter);
            } else {
                communityFilter = getCurrentUserCommunity(currentUserId, userRole);
                log.info("🏘️ [统计接口-管家/管理员] 查询到的用户所在小区: {}", communityFilter);
            }
        }

        Map<String, Object> result = violationsService.getViolationStatistics(parsedStartDate, parsedEndDate, plateNumber, createdByFilter, communityFilter);
        return Result.success(result);
    }

    @GetMapping("/high-risk-vehicles")
    @ApiOperation("获取高风险车辆列表")
    public Result<List<Map<String, Object>>> getHighRiskVehicles(
            @ApiParam("开始时间") @RequestParam(required = false) String startDate,
            @ApiParam("结束时间") @RequestParam(required = false) String endDate,
            @ApiParam("限制数量") @RequestParam(defaultValue = "10") Integer limit,
            @ApiParam("用户角色") @RequestParam(required = false) String userRole,
            @ApiParam("小区名称") @RequestParam(required = false) String park_name,
            @ApiParam("创建者") @RequestParam(required = false) String created_by,
            HttpServletRequest request) {
        // 解析日期参数
        LocalDateTime parsedStartDate = parseDateTime(startDate, true);
        LocalDateTime parsedEndDate = parseDateTime(endDate, false);

        // 获取当前用户ID
        String currentUserId = getCurrentUserId(request);

        // 根据用户角色决定查询范围
        String createdByFilter = null;
        String communityFilter = null;

        if ("patrol".equals(userRole) || "resident".equals(userRole)) {
            // 普通用户只能查看基于自己创建记录的高风险车辆
            createdByFilter = currentUserId;
        } else if ("housekeeper".equals(userRole) || "manager".equals(userRole)) {
            // 🆕 优先使用前端传递的created_by参数（用于过滤当前用户创建的记录）
            if (created_by != null && !created_by.trim().isEmpty()) {
                createdByFilter = created_by.trim();
                log.info("👤 [高风险车辆接口-管家/管理员] 使用前端传递的创建者参数: {}", createdByFilter);
            }

            // 管家和管理员需要根据小区过滤数据
            if (park_name != null && !park_name.trim().isEmpty()) {
                communityFilter = park_name.trim();
                log.info("🏘️ [高风险车辆接口-管家/管理员] 使用前端传递的小区参数: {}", communityFilter);
            } else {
                communityFilter = getCurrentUserCommunity(currentUserId, userRole);
                log.info("🏘️ [高风险车辆接口-管家/管理员] 查询到的用户所在小区: {}", communityFilter);
            }
        }

        List<Map<String, Object>> result = violationsService.getHighRiskVehicles(parsedStartDate, parsedEndDate, limit, createdByFilter, communityFilter);
        return Result.success(result);
    }

    @GetMapping("/owners/by-plate/{plateNumber}")
    @ApiOperation("根据车牌号查询车主信息")
    public Result<Map<String, Object>> getOwnerByPlateNumber(
            @ApiParam("车牌号") @PathVariable String plateNumber) {
        Map<String, Object> result = violationsService.getOwnerByPlateNumber(plateNumber);
        return result != null ? Result.success(result) : Result.error("车主信息不存在");
    }

    @GetMapping("/owners/plate-suggestions")
    @ApiOperation("车牌号搜索建议")
    public Result<List<Map<String, Object>>> getPlateSuggestions(
            @ApiParam("关键词") @RequestParam String keyword,
            @ApiParam("巡逻员编码") @RequestParam(required = false) String usercode) {

        List<Map<String, Object>> result = violationsService.getPlateSuggestions(keyword, usercode);
        return Result.success(result);
    }

    @GetMapping("/violation-plate-suggestions")
    @ApiOperation("🆕 从违规记录中获取车牌号搜索建议")
    public Result<List<Map<String, Object>>> getViolationPlateSuggestions(
            @ApiParam("关键词") @RequestParam String keyword,
            @ApiParam("车场代码或车场名称（支持多个，逗号分隔，用于权限过滤）") @RequestParam(required = false) String parkCode) {

        List<Map<String, Object>> result = violationsService.getViolationPlateSuggestions(keyword, parkCode);
        return Result.success(result);
    }

    @GetMapping("/appointment-records/{plateNumber}")
    @ApiOperation("根据车牌号查询预约记录（用于违规录入）")
    public Result<List<Map<String, Object>>> getAppointmentRecords(
            @ApiParam("车牌号") @PathVariable String plateNumber) {

        List<Map<String, Object>> result = violationsService.getAppointmentRecordsByPlate(plateNumber);
        return Result.success(result);
    }

    @GetMapping("/appointment-detail/{appointmentId}")
    @ApiOperation("🆕 根据预约ID查询预约详细信息（小程序端专用）")
    public Result<Map<String, Object>> getAppointmentDetail(
            @ApiParam("预约记录ID") @PathVariable Integer appointmentId) {

        log.info("🔍 [查询预约详情] appointmentId={}", appointmentId);

        if (appointmentId == null) {
            return Result.error("预约ID不能为空");
        }

        try {
            Map<String, Object> appointmentDetail = violationsService.getAppointmentDetail(appointmentId);
            if (appointmentDetail != null) {
                log.info("✅ [预约详情查询成功] appointmentId={}, ownerName={}, appointmentType={}",
                        appointmentId,
                        appointmentDetail.get("ownerName"),
                        appointmentDetail.get("appointmentType"));
                return Result.success(appointmentDetail);
            } else {
                log.warn("⚠️ [预约详情不存在] appointmentId={}", appointmentId);
                return Result.error("预约记录不存在");
            }
        } catch (Exception e) {
            log.error("❌ [预约详情查询异常] appointmentId={}, error={}", appointmentId, e.getMessage());
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @GetMapping("/appointment-analysis/{plateNumber}")
    @ApiOperation("根据车牌号分析违规情况")
    public Result<Map<String, Object>> analyzeViolationByPlate(
            @ApiParam("车牌号") @PathVariable String plateNumber) {

        Map<String, Object> result = violationsService.analyzeViolationByPlate(plateNumber);
        return Result.success(result);
    }

    @GetMapping("/owners/{ownerId}/vehicles")
    @ApiOperation("获取车主的车辆列表")
    public Result<List<Map<String, Object>>> getOwnerVehicles(
            @ApiParam("车主ID") @PathVariable Integer ownerId) {

        List<Map<String, Object>> result = violationsService.getOwnerVehicles(ownerId);
        return Result.success(result);
    }

    @PutMapping("/owners/{ownerId}/credit-score")
    @ApiOperation("更新车主信用分")
    public Result<Boolean> updateOwnerCreditScore(
            @ApiParam("车主ID") @PathVariable Integer ownerId,
            @ApiParam("信用分") @RequestParam Integer creditScore) {

        boolean result = violationsService.updateOwnerCreditScore(ownerId, creditScore);
        return result ? Result.success(true) : Result.error("更新失败");
    }

    /**
     * 从请求中获取当前用户ID，并根据ID查询用户名用于匹配violations表的created_by字段
     */
    private String getCurrentUserId(HttpServletRequest request) {
        try {
            // 🔧 优先使用TokenUtils获取当前用户信息
            User currentUser = TokenUtils.getCurrentUser();
            if (currentUser != null && currentUser.getUserName() != null) {
                String userName = currentUser.getUserName();
                log.info("✅ 通过TokenUtils获取到用户信息: userId={}, userName={}", currentUser.getUserId(), userName);
                // 返回用户名，用于匹配violations表的created_by字段
                return userName;
            } else {
                log.warn("⚠️ TokenUtils返回的用户信息为空或用户名为空");
            }
        } catch (Exception e) {
            log.error("❌ 通过TokenUtils获取用户信息失败: {}", e.getMessage());
        }

        // 🔧 备用方案：从请求头中获取用户ID
        String userId = request.getHeader("User-Id");
        String userNameEncoded = request.getHeader("User-Name-Encoded"); // Base64编码的用户姓名

        if (userId != null && !userId.trim().isEmpty()) {
            try {
                // 🆕 根据用户ID查询用户信息，获取用户名
                Integer userIdInt = Integer.parseInt(userId.trim());
                log.info("🔍 尝试根据User-Id={}查询用户信息", userIdInt);

                User user = userService.getById(userIdInt);

                if (user != null && user.getUserName() != null) {
                    String userName = user.getUserName();
                    log.info("✅ 根据User-Id={}查询到用户信息: userName={}", userId, userName);
                    // 返回用户名，用于匹配violations表的created_by字段
                    return userName;
                } else {
                    log.warn("⚠️ 根据User-Id={}未查询到用户信息或用户名为空", userId);
                    // 尝试从violations表中查询该用户创建的记录，获取用户名
                    String userNameFromViolations = getCreatedByFromViolations(userIdInt);
                    if (userNameFromViolations != null) {
                        log.info("✅ 从violations表获取到创建者信息: {}", userNameFromViolations);
                        return userNameFromViolations;
                    }
                }
            } catch (NumberFormatException e) {
                log.warn("⚠️ User-Id格式错误: {}", userId);
            } catch (Exception e) {
                log.error("❌ 根据User-Id查询用户信息失败: {}", e.getMessage(), e);
                // 尝试从violations表中查询该用户创建的记录，获取用户名
                try {
                    Integer userIdInt = Integer.parseInt(userId.trim());
                    String userNameFromViolations = getCreatedByFromViolations(userIdInt);
                    if (userNameFromViolations != null) {
                        log.info("✅ 从violations表获取到创建者信息: {}", userNameFromViolations);
                        return userNameFromViolations;
                    }
                } catch (Exception ex) {
                    log.error("❌ 从violations表查询创建者信息也失败: {}", ex.getMessage());
                }
            }

            // 🔧 修复：如果查询失败，尝试解码用户姓名，如果有的话优先使用姓名
            String decodedName = decodeUserName(userNameEncoded);
            if (decodedName != null && !decodedName.trim().isEmpty()) {
                log.info("从请求头获取到用户信息: User-Id={}, User-Name={}", userId, decodedName);
                log.info("✅ 优先使用解码后的用户姓名: {}", decodedName);
                return decodedName.trim();
            } else {
                log.warn("⚠️ 无法解码用户姓名，使用User-Id作为fallback: {}", userId);
                return userId.trim();
            }
        }

        // 🔧 如果有编码的用户姓名但没有ID，解码并使用姓名（避免返回anonymous）
        if (userNameEncoded != null && !userNameEncoded.trim().isEmpty()) {
            String decodedName = decodeUserName(userNameEncoded);
            if (decodedName != null && !decodedName.trim().isEmpty()) {
                log.info("未获取到User-Id，但有编码的User-Name，使用姓名作为用户标识: {}", decodedName);
                return decodedName.trim();
            }
        }

        log.warn("无法获取用户信息，使用默认值 anonymous");
        return "anonymous";
    }



    /**
     * 解码用户姓名（支持Base64和URL编码）
     */
    private String decodeUserName(String encodedName) {
        if (encodedName == null || encodedName.trim().isEmpty()) {
            return null;
        }

        try {
            String trimmedName = encodedName.trim();

            // 🆕 先尝试URL解码（如果包含%字符）
            if (trimmedName.contains("%")) {
                try {
                    String urlDecoded = java.net.URLDecoder.decode(trimmedName, "UTF-8");
                    log.debug("用户姓名URL解码: {} -> {}", encodedName, urlDecoded);
                    return urlDecoded;
                } catch (Exception urlEx) {
                    log.warn("URL解码失败，尝试Base64解码: {}", urlEx.getMessage());
                }
            }

            // 如果URL解码失败或不包含%，尝试Base64解码
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(trimmedName);
            String decoded = new String(decodedBytes, "UTF-8");
            log.debug("用户姓名Base64解码: {} -> {}", encodedName, decoded);
            return decoded;

        } catch (Exception e) {
            log.warn("用户姓名解码失败: {}, 错误: {}", encodedName, e.getMessage());
            return null;
        }
    }





    /**
     * 🆕 通过业主姓名关联查询预约记录
     * 关联ownerinfo表和appointment表，筛选与当前巡逻员相同车场的数据
     *
     * @param keyword 搜索关键词（车牌号或业主姓名）
     * @param page 页码
     * @param size 每页数量
     * @return 预约记录列表
     */
    @GetMapping("/appointment-records-by-owner")
    @ApiOperation("通过业主信息关联查询预约记录")
    public Result<List<Map<String, Object>>> getAppointmentRecordsByOwner(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @ApiParam("巡逻员编码") @RequestParam(required = false) String usercode) {

        try {
            log.info("🔍 [预约记录关联查询] 开始查询: keyword={}, page={}, size={}, usercode={}", keyword, page, size, usercode);

            // 调用Service层方法
            List<Map<String, Object>> records = violationsService.getAppointmentRecordsByOwnerInfo(
                keyword, page, size, usercode);

            log.info("✅ [预约记录关联查询] 查询完成: 共{}条记录", records.size());

            return Result.success(records);

        } catch (Exception e) {
            log.error("❌ [预约记录关联查询] 查询失败: {}", e.getMessage(), e);
            return Result.error("查询预约记录失败: " + e.getMessage());
        }
    }



    /**
     * 🔧 从violations表中查询指定用户ID创建的记录，获取创建者名称
     * 这是一个备选方案，当user表查询失败时使用
     *
     * @param userId 用户ID
     * @return 创建者名称，如果查询失败返回null
     */
    private String getCreatedByFromViolations(Integer userId) {
        try {
            log.info("🔍 尝试从violations表查询User-Id={}的创建者信息", userId);

            // 使用MyBatis-Plus的查询方法，查询该用户创建的违规记录
            // 由于createdBy字段是String类型，我们需要查询是否有该用户ID对应的记录
            // 这里我们查询最近的一条记录，获取创建者信息
            List<Violations> violations = violationsService.list(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Violations>()
                    .eq("reporter_id", userId)
                    .orderByDesc("created_at")
                    .last("LIMIT 1")
            );

            if (!violations.isEmpty()) {
                String createdBy = violations.get(0).getCreatedBy();
                log.info("✅ 从violations表查询到User-Id={}的创建者信息: {}", userId, createdBy);
                return createdBy;
            } else {
                log.warn("⚠️ 在violations表中未找到User-Id={}创建的记录", userId);
                return null;
            }

        } catch (Exception e) {
            log.error("❌ 从violations表查询创建者信息失败: {}", e.getMessage(), e);
            return null;
        }
    }



    /**
     * 解析日期时间字符串，支持多种格式
     * @param dateStr 日期字符串
     * @param isStartDate 是否为开始日期（开始日期设为00:00:00，结束日期设为23:59:59）
     * @return LocalDateTime对象，如果解析失败返回null
     */
    private LocalDateTime parseDateTime(String dateStr, boolean isStartDate) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            // 尝试解析完整的日期时间格式 yyyy-MM-dd HH:mm:ss
            if (dateStr.length() > 10) {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }

            // 解析只有日期的格式 yyyy-MM-dd
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // 如果是开始日期，设置为当天的00:00:00
            if (isStartDate) {
                return date.atStartOfDay();
            } else {
                // 如果是结束日期，设置为当天的23:59:59
                return date.atTime(23, 59, 59);
            }

        } catch (Exception e) {
            // 如果解析失败，记录日志并返回null
            System.err.println("日期解析失败: " + dateStr + ", 错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 🔧 诊断patrol表查询问题
     */
    @GetMapping("/debug/patrol")
    @ApiOperation("诊断patrol表查询问题")
    public Result<String> diagnosisPatrolQuery() {
        try {
            log.info("🔧 [调试接口] 开始诊断patrol表查询");

            // 调用Service中的诊断方法
            if (violationsService instanceof com.parkingmanage.service.impl.ViolationsServiceImpl) {
                ((com.parkingmanage.service.impl.ViolationsServiceImpl) violationsService).diagnosisPatrolQuery();
            }

            return Result.success("诊断完成，请查看日志");
        } catch (Exception e) {
            log.error("❌ 诊断patrol表查询失败: {}", e.getMessage(), e);
            return Result.error("诊断失败: " + e.getMessage());
        }
    }

    /**
     * 🆕 获取当前用户所在小区/社区信息
     * @param currentUserId 当前用户ID
     * @param userRole 用户角色
     * @return 小区名称，如果未找到返回null
     */
    private String getCurrentUserCommunity(String currentUserId, String userRole) {
        try {
            log.info("🏘️ [获取用户小区] 开始查询 - userId: {}, role: {}", currentUserId, userRole);

            if (currentUserId == null || "anonymous".equals(currentUserId)) {
                log.warn("⚠️ [获取用户小区] 用户ID无效: {}", currentUserId);
                return null;
            }
            String community = null;
            // 根据角色查询不同的表
            if ("housekeeper".equals(userRole)) {
                // 管家从butler表查询
                community = violationsService.getButlerCommunity(currentUserId);
                log.info("🏠 [管家小区] 查询结果: {}", community);
            } else if ("manager".equals(userRole)) {
                // 管理员可能需要查询其他表，暂时返回null表示查看所有小区
                log.info("👔 [管理员] 暂时不限制小区，可查看所有数据");
                return null;
            }

            if (community == null || community.trim().isEmpty()) {
                log.warn("⚠️ [获取用户小区] 未找到用户小区信息 - userId: {}, role: {}", currentUserId, userRole);
                return null;
            }

            log.info("✅ [获取用户小区] 成功获取小区: {}", community);
            return community.trim();

        } catch (Exception e) {
            log.error("❌ [获取用户小区] 查询失败 - userId: {}, role: {}, error: {}",
                     currentUserId, userRole, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 🔔 发送违规停车微信通知
     * @param violation 违规记录
     * @return 微信发送结果，包含success和message字段
     */
    private Map<String, Object> sendViolationNotification(Violations violation) {
        Map<String, Object> notifyResult = new HashMap<>();
        notifyResult.put("success", true);
        notifyResult.put("message", "");
        
        try {
            // 参数校验
            if (violation.getPlateNumber() == null || violation.getPlateNumber().trim().isEmpty()) {
                log.warn("⚠️ [违规通知跳过] 车牌号为空");
                notifyResult.put("success", false);
                notifyResult.put("message", "车牌号为空，无法发送通知");
                return notifyResult;
            }

            // 如果没有预约记录ID，则跳过通知
            if (violation.getAppointmentId() == null) {
                log.warn("⚠️ [违规通知跳过] 无预约记录ID，无法发送通知 - 车牌: {}", violation.getPlateNumber());
                notifyResult.put("success", false);
                notifyResult.put("message", "无预约记录，无法发送通知");
                return notifyResult;
            }

            // 📋 直接查询appointment对象
            com.parkingmanage.entity.Appointment appointment = appointmentService.getById(violation.getAppointmentId());
            if (appointment == null) {
                log.warn("⚠️ [违规通知跳过] 未找到预约记录 - appointmentId: {}", violation.getAppointmentId());
                notifyResult.put("success", false);
                notifyResult.put("message", "未找到预约记录，无法发送通知");
                return notifyResult;
            }

            log.info("📝 [违规通知] 找到预约记录 - appointmentId: {}, 预约类型: {}, 车牌: {}",
                violation.getAppointmentId(), appointment.getAppointtype(), appointment.getPlatenumber());

            // 构建通知参数
            String plateNumber = violation.getPlateNumber();
            String parkName = violation.getParkName() != null ? violation.getParkName() : "停车场";
            String violationLocation = violation.getLocation() != null ? violation.getLocation() : "未知位置";

            // 计算停车时长（从进场时间到当前时间）
            String parkingDuration = "未知";
            if (violation.getEnterTime() != null) {
                try {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime enterTime = violation.getEnterTime();

                    // 计算时间差（总秒数）
                    long totalSeconds = java.time.Duration.between(enterTime, now).getSeconds();

                    if (totalSeconds < 0) {
                        parkingDuration = "时间异常";
                        log.warn("⚠️ 进场时间晚于当前时间: enterTime={}, now={}", enterTime, now);
                    } else {
                        // 计算小时、分钟、秒
                        long hours = totalSeconds / 3600;
                        long minutes = (totalSeconds % 3600) / 60;
                        long seconds = totalSeconds % 60;

                        // 构建停车时长字符串
                        StringBuilder durationBuilder = new StringBuilder();

                        if (hours > 0) {
                            durationBuilder.append(hours).append("小时");
                        }
                        if (minutes > 0) {
                            durationBuilder.append(minutes).append("分钟");
                        }
                        if (seconds > 0 || durationBuilder.length() == 0) {
                            durationBuilder.append(seconds).append("秒");
                        }

                        parkingDuration = durationBuilder.toString();
                    }

                    log.info("🕐 [停车时长] 车牌: {}, 进场时间: {}, 当前时间: {}, 停车时长: {}",
                            plateNumber, enterTime, now, parkingDuration);
                } catch (Exception e) {
                    log.warn("⚠️ 计算停车时长失败: {}", e.getMessage());
                    parkingDuration = "计算失败";
                }
            } else {
                log.warn("⚠️ 进场时间为空，无法计算停车时长 - 车牌: {}", plateNumber);
            }

            // 📌 根据预约类型确定通知对象
            String appointType = appointment.getAppointtype();
            int successCount = 0;
            int totalCount = 0;

            // ⏭️ 后台预约：跳过微信通知
            if ("后台".equals(appointType) || "backend".equalsIgnoreCase(appointType)) {
                log.info("⏭️ [后台预约] 跳过微信通知 - 车牌: {}, appointmentId: {}", plateNumber, violation.getAppointmentId());
                notifyResult.put("success", true);
                notifyResult.put("message", "后台预约，无需发送微信通知");
                return notifyResult;
            }

            // ✅ 邀请预约：通知访客+管家+业主
            if ("邀请".equals(appointType)) {
                log.info("📧 [邀请预约] 发送违规通知给：访客、管家、业主");

                // 1. 通知访客
                if (appointment.getOpenid() != null && !appointment.getOpenid().trim().isEmpty()) {
                    totalCount++;
                    if (sendViolationNotificationToUser(appointment.getOpenid(), plateNumber, parkName,
                            violationLocation, parkingDuration, "违规通知(访客)")) {
                        successCount++;
                    }
                }

                // 2. 通知管家
                if (appointment.getAuditopenid() != null && !appointment.getAuditopenid().trim().isEmpty()) {
                    totalCount++;
                    if (sendViolationNotificationToUser(appointment.getAuditopenid(), plateNumber, parkName,
                            violationLocation, parkingDuration, "违规通知(管家)")) {
                        successCount++;
                    }
                }

                // 3. 通知业主
                if (appointment.getOwneropenid() != null && !appointment.getOwneropenid().trim().isEmpty()) {
                    totalCount++;
                    if (sendViolationNotificationToUser(appointment.getOwneropenid(), plateNumber, parkName,
                            violationLocation, parkingDuration, "违规通知(业主)")) {
                        successCount++;
                    }
                }
            }
            // ✅ 代人预约：通知管家+业主
            else if ("代人".equals(appointType)) {
                log.info("📧 [代人预约] 发送违规通知给：管家、业主");

                // 1. 通知管家
                if (appointment.getAuditopenid() != null && !appointment.getAuditopenid().trim().isEmpty()) {
                    totalCount++;
                    if (sendViolationNotificationToUser(appointment.getAuditopenid(), plateNumber, parkName,
                            violationLocation, parkingDuration, "违规通知(管家)")) {
                        successCount++;
                    }
                }

                // 2. 通知业主
                if (appointment.getOwneropenid() != null && !appointment.getOwneropenid().trim().isEmpty()) {
                    totalCount++;
                    if (sendViolationNotificationToUser(appointment.getOwneropenid(), plateNumber, parkName,
                            violationLocation, parkingDuration, "违规通知(业主)")) {
                        successCount++;
                    }
                }
            }
            // ✅ 其他类型（自助、业主预约）：仅通知访客
            else {
                log.info("📧 [{}预约] 发送违规通知给：访客", appointType);
                if (appointment.getOpenid() != null && !appointment.getOpenid().trim().isEmpty()) {
                    totalCount++;
                    if (sendViolationNotificationToUser(appointment.getOpenid(), plateNumber, parkName,
                            violationLocation, parkingDuration, "违规通知(访客)")) {
                        successCount++;
                    }
                }
            }

            log.info("📊 [违规通知] 发送完成 - 车牌: {}, 预约类型: {}, 成功: {}/{}",
                plateNumber, appointType, successCount, totalCount);

            // 根据发送结果返回
            if (totalCount == 0) {
                notifyResult.put("success", false);
                notifyResult.put("message", "未找到可通知的用户");
            } else if (successCount == 0) {
                notifyResult.put("success", false);
                notifyResult.put("message", String.format("所有微信通知发送失败（共%d个）", totalCount));
            } else if (successCount < totalCount) {
                notifyResult.put("success", false);
                notifyResult.put("message", String.format("部分微信通知发送失败（成功%d个，失败%d个）", successCount, totalCount - successCount));
            } else {
                notifyResult.put("success", true);
                notifyResult.put("message", String.format("微信通知全部发送成功（共%d个）", successCount));
            }
            
            return notifyResult;

        } catch (Exception e) {
            log.error("❌ [违规通知发送异常] 车牌: {}, appointmentId: {}, 错误: {}",
                    violation.getPlateNumber(), violation.getAppointmentId(), e.getMessage(), e);
            notifyResult.put("success", false);
            notifyResult.put("message", "微信通知发送异常：" + e.getMessage());
            return notifyResult;
        }
    }

    /**
     * 🔔 发送违规通知给指定用户
     * @param openid 用户openid
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @param violationLocation 违规地点
     * @param parkingDuration 停车时长
     * @param notificationType 通知类型描述
     * @return 是否发送成功
     */
    private boolean sendViolationNotificationToUser(String openid, String plateNumber, String parkName,
                                                     String violationLocation, String parkingDuration,
                                                     String notificationType) {
        try {
            // 注意：这里需要传openid而不是nickname，需要修改WeChatTemplateMessageService的接口
            // 暂时使用一个临时方案，直接调用模板消息发送
            Map<String, Object> result = weChatTemplateMessageService.sendParkingViolationNotification(
                plateNumber,
                parkName,
                violationLocation,
                parkingDuration,
                openid  // 这里传入openid，但接口期望nickname，需要修改Service层
            );

            if (result != null && Boolean.TRUE.equals(result.get("success"))) {
                log.info("✅ [违规通知] {}发送成功 - openid: {}", notificationType, openid);
                return true;
            } else {
                String message = result != null ? (String) result.get("message") : "未知错误";
                log.warn("⚠️ [违规通知] {}发送失败 - openid: {}, 原因: {}", notificationType, openid, message);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ [违规通知] {}发送异常 - openid: {}", notificationType, openid, e);
            return false;
        }
    }


    /**
     * 🔍 获取违规记录对应的管家昵称
     * @param violation 违规记录
     * @return 管家昵称
     */
    private String getManagerNicknameForViolation(Violations violation) {
        try {
            // 方案1：根据停车场名称查询对应的管家
            if (violation.getParkName() != null && !violation.getParkName().trim().isEmpty()) {
                // 这里可以调用Service查询停车场对应的管家
                // 暂时返回一个默认值，实际项目中需要根据业务逻辑实现
                log.info("🔍 根据停车场 {} 查询管家信息", violation.getParkName());

                // TODO: 实现根据停车场查询管家的逻辑
                // String managerNickname = violationsService.getManagerByParkName(violation.getParkName());
                // if (managerNickname != null) return managerNickname;
            }

            // 方案2：根据创建者查询对应的管家（如果创建者是管家）
            if (violation.getCreatedBy() != null && !violation.getCreatedBy().trim().isEmpty()) {
                log.info("🔍 使用创建者作为管家: {}", violation.getCreatedBy());
                return violation.getCreatedBy();
            }
            // 方案3：使用默认管家（用于测试）
            log.warn("⚠️ 未找到具体管家，使用默认管家");
            return "测试管家";

        } catch (Exception e) {
            log.error("❌ 查询管家信息异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 🆕 获取月票车超时配置
     */
    @ApiOperation("获取月票车超时配置")
    @GetMapping("/monthly-ticket-config")
    public Result getMonthlyTicketTimeoutConfig(
            @ApiParam("停车场编码") @RequestParam String parkCode) {
        try {
            log.info("🔧 [获取月票车超时配置] parkCode={}", parkCode);

            Map<String, Object> config = violationsService.getMonthlyTicketTimeoutConfig(parkCode);

            log.info("✅ [配置获取成功] parkCode={}, config={}", parkCode, config);
            return Result.success(config);

        } catch (Exception e) {
            log.error("❌ [配置获取失败] parkCode={}, error={}", parkCode, e.getMessage(), e);
            return Result.error("获取配置失败: " + e.getMessage());
        }
    }

    /**
     * 🆕 获取月票车超时配置（路径参数版本）
     */
    @ApiOperation("获取月票车超时配置（路径参数版本）")
    @GetMapping("/monthly-ticket-config/{parkCode}")
    public Result getMonthlyTicketTimeoutConfigByPath(
            @ApiParam("停车场编码") @PathVariable String parkCode) {
        try {
            log.info("🔧 [获取月票车超时配置-路径参数] parkCode={}", parkCode);

            Map<String, Object> config = violationsService.getMonthlyTicketTimeoutConfig(parkCode);

            log.info("✅ [配置获取成功] parkCode={}, config={}", parkCode, config);
            return Result.success(config);

        } catch (Exception e) {
            log.error("❌ [配置获取失败] parkCode={}, error={}", parkCode, e.getMessage(), e);
            return Result.error("获取配置失败: " + e.getMessage());
        }
    }

    /**
     * 🆕 保存月票车超时配置
     */
    @ApiOperation("保存月票车超时配置")
    @PostMapping("/monthly-ticket-config")
    public Result saveMonthlyTicketTimeoutConfig(
            @ApiParam("停车场编码") @RequestParam String parkCode,
            @ApiParam("超时时间（分钟）") @RequestParam Integer timeoutMinutes,
            @ApiParam("最大违规次数") @RequestParam Integer maxViolationCount,
            HttpServletRequest request) {
        try {
            // 获取当前操作员信息
            String token = request.getHeader("token");
            User currentUser = TokenUtils.getCurrentUser();
            String operatorId = currentUser != null ? String.valueOf(currentUser.getUserId()) : "UNKNOWN";

            log.info("💾 [保存月票车超时配置] parkCode={}, timeout={}分钟, maxCount={}, operator={}",
                    parkCode, timeoutMinutes, maxViolationCount, operatorId);

            // 参数验证
            if (timeoutMinutes <= 0) {
                return Result.error("超时时间必须大于0");
            }
            if (maxViolationCount <= 0) {
                return Result.error("最大违规次数必须大于0");
            }

            boolean result = violationsService.saveMonthlyTicketTimeoutConfig(
                    parkCode, timeoutMinutes, maxViolationCount, operatorId);

            if (result) {
                log.info("✅ [配置保存成功] parkCode={}", parkCode);
                return Result.success("配置保存成功");
            } else {
                log.error("❌ [配置保存失败] parkCode={}", parkCode);
                return Result.error("配置保存失败");
            }

        } catch (Exception e) {
            log.error("❌ [配置保存异常] parkCode={}, error={}", parkCode, e.getMessage(), e);
            return Result.error("配置保存失败: " + e.getMessage());
        }
    }

    /**
     * 🆕 保存万象上东配置（JSON格式）
     */
    @ApiOperation("保存万象上东月票车配置")
    @PostMapping("/monthly-ticket-config/save-full")
    public Result saveWanXiangConfig(@RequestBody Map<String, Object> configData,
                                      HttpServletRequest request) {
        try {
            // 获取当前操作员信息
            User currentUser = TokenUtils.getCurrentUser();
            String operatorId = currentUser != null ? String.valueOf(currentUser.getUserId()) : "UNKNOWN";
            
            // 提取配置参数
            String parkCode = (String) configData.get("parkCode");
            String parkName = (String) configData.get("parkName");
            String nightStartTime = (String) configData.get("nightStartTime");
            String nightEndTime = (String) configData.get("nightEndTime");
            Integer nightTimeHours = (Integer) configData.get("nightTimeHours");
            Boolean enableOvernightCheck = (Boolean) configData.getOrDefault("enableOvernightCheck", true);
            String description = (String) configData.get("description");
            
            log.info("💾 [保存万象上东配置] parkCode={}, nightTime={}-{}, hours={}, enabled={}, operator={}",
                    parkCode, nightStartTime, nightEndTime, nightTimeHours, enableOvernightCheck, operatorId);
            
            // 参数验证
            if (parkCode == null || parkCode.trim().isEmpty()) {
                return Result.error("车场编码不能为空");
            }
            if (nightTimeHours == null || nightTimeHours <= 0) {
                return Result.error("夜间时长必须大于0小时");
            }
            
            // 调用服务保存配置
            boolean result = violationsService.saveWanXiangConfig(
                    parkCode, parkName, nightStartTime, nightEndTime, 
                    nightTimeHours, enableOvernightCheck, description, operatorId);
            
            if (result) {
                log.info("✅ [万象上东配置保存成功] parkCode={}", parkCode);
                return Result.success("配置保存成功");
            } else {
                log.error("❌ [万象上东配置保存失败] parkCode={}", parkCode);
                return Result.error("配置保存失败");
            }
            
        } catch (Exception e) {
            log.error("❌ [保存万象上东配置异常] error={}", e.getMessage(), e);
            return Result.error("配置保存失败: " + e.getMessage());
        }
    }

    /**
     * 🆕 保存月票车完整配置（包含过夜停车配置）
     */
    @ApiOperation("保存月票车完整配置")
    @PostMapping("/monthly-ticket-full-config")
    public Result saveMonthlyTicketFullConfig(
            @ApiParam("停车场编码") @RequestParam String parkCode,
            @ApiParam("超时时间（分钟）") @RequestParam Integer timeoutMinutes,
            @ApiParam("最大违规次数") @RequestParam Integer maxViolationCount,
            @ApiParam("过夜判定时长（小时）") @RequestParam Integer overnightTimeHours,
            @ApiParam("是否启用过夜检查") @RequestParam(defaultValue = "true") Boolean enableOvernightCheck,
            @ApiParam("免检月票类型列表") @RequestParam(required = false) java.util.List<String> exemptTicketTypes,
            HttpServletRequest request) {
        try {
            // 获取当前操作员信息
            String token = request.getHeader("token");
            User currentUser = TokenUtils.getCurrentUser();
            String operatorId = currentUser != null ? String.valueOf(currentUser.getUserId()) : "UNKNOWN";

            // 🔍 详细调试日志
            log.info("🔍 [调试] 接收到的免检类型参数: {}", exemptTicketTypes);
            log.info("🔍 [调试] 免检类型参数类型: {}", exemptTicketTypes != null ? exemptTicketTypes.getClass().getName() : "null");
            log.info("🔍 [调试] 免检类型参数大小: {}", exemptTicketTypes != null ? exemptTicketTypes.size() : "null");
            if (exemptTicketTypes != null && !exemptTicketTypes.isEmpty()) {
                for (int i = 0; i < exemptTicketTypes.size(); i++) {
                    log.info("🔍 [调试] 免检类型[{}]: {}", i, exemptTicketTypes.get(i));
                }
            }

            log.info("💾 [保存月票车完整配置] parkCode={}, timeout={}分钟, maxCount={}, overnightHours={}, enabled={}, exempt={}, operator={}",
                    parkCode, timeoutMinutes, maxViolationCount, overnightTimeHours,
                    enableOvernightCheck, exemptTicketTypes, operatorId);

            // 参数验证
            if (timeoutMinutes <= 0) {
                return Result.error("超时时间必须大于0");
            }
            if (maxViolationCount <= 0) {
                return Result.error("最大违规次数必须大于0");
            }
            if (overnightTimeHours <= 0) {
                return Result.error("过夜判定时长必须大于0小时");
            }

            // 处理免检类型（确保不为null）
            if (exemptTicketTypes == null) {
                exemptTicketTypes = new java.util.ArrayList<>();
            }

            boolean result = violationsService.saveMonthlyTicketFullConfigWithExempt(
                    parkCode, timeoutMinutes, maxViolationCount,
                    overnightTimeHours, enableOvernightCheck, exemptTicketTypes, operatorId);

            if (result) {
                log.info("✅ [完整配置保存成功] parkCode={}", parkCode);
                return Result.success("配置保存成功");
            } else {
                log.error("❌ [完整配置保存失败] parkCode={}", parkCode);
                return Result.error("配置保存失败");
            }

        } catch (Exception e) {
            log.error("❌ [保存月票车完整配置异常] parkCode={}, error={}", parkCode, e.getMessage(), e);
            return Result.error("保存配置失败: " + e.getMessage());
        }
    }


    /**
     * 🆕 获取月票车违规统计
     */
    @ApiOperation("获取月票车违规统计")
    @GetMapping("/monthly-ticket-violations")
    public Result getMonthlyTicketViolations(
            @ApiParam("停车场编码") @RequestParam(required = false) String parkCode,
            @ApiParam("车牌号") @RequestParam(required = false) String plateNumber,
            @ApiParam("开始日期") @RequestParam(required = false) String startDate,
            @ApiParam("结束日期") @RequestParam(required = false) String endDate,
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("页大小") @RequestParam(defaultValue = "10") Integer size) {
        try {
            log.info("📊 [查询月票车违规] parkCode={}, plateNumber={}, startDate={}, endDate={}",
                    parkCode, plateNumber, startDate, endDate);

            // 构建查询条件（这里可以扩展查询逻辑）
            // 暂时返回基本信息，后续可以根据需要扩展
            Map<String, Object> result = new HashMap<>();
            result.put("message", "月票车违规查询功能");
            result.put("parkCode", parkCode != null ? parkCode : "ALL");
            result.put("plateNumber", plateNumber != null ? plateNumber : "ALL");

            return Result.success(result);

        } catch (Exception e) {
            log.error("❌ [查询月票车违规异常] error={}", e.getMessage(), e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 🆕 保存或更新过夜停车配置（专门针对新版过夜规则）
     */
    @ApiOperation("保存或更新过夜停车配置")
    @PostMapping("/overnight-config")
    public Result saveOvernightConfig(
            @ApiParam("停车场编码") @RequestParam String parkCode,
            @ApiParam("停车场名称") @RequestParam String parkName,
            @ApiParam("超时时间（分钟）") @RequestParam Integer timeoutMinutes,
            @ApiParam("最大违规次数") @RequestParam Integer maxViolationCount,
            @ApiParam("过夜时长限制（小时）") @RequestParam Integer overnightTimeHours,
            @ApiParam("是否启用过夜检查") @RequestParam(defaultValue = "true") Boolean enableOvernightCheck,
            HttpServletRequest request) {
        try {
            // 获取当前操作员信息
            User currentUser = TokenUtils.getCurrentUser();
            String operatorId = currentUser != null ? String.valueOf(currentUser.getUserId()) : "system";

            log.info("💾 [保存过夜配置] parkCode={}, parkName={}, timeout={}分钟, maxCount={}, overnightHours={}, enabled={}, operator={}",
                    parkCode, parkName, timeoutMinutes, maxViolationCount, overnightTimeHours, enableOvernightCheck, operatorId);

            // 参数验证
            if (timeoutMinutes <= 0) {
                return Result.error("超时时间必须大于0");
            }
            if (maxViolationCount <= 0) {
                return Result.error("最大违规次数必须大于0");
            }
            if (overnightTimeHours <= 0) {
                return Result.error("过夜时长限制必须大于0小时");
            }

            boolean success = monthlyTicketTimeoutConfigService.saveOrUpdateOvernightConfig(
                    parkCode, parkName, timeoutMinutes, maxViolationCount,
                    overnightTimeHours, enableOvernightCheck, operatorId);

            if (success) {
                log.info("✅ [过夜配置保存成功] parkCode={}", parkCode);
                return Result.success("过夜配置保存成功");
            } else {
                log.error("❌ [过夜配置保存失败] parkCode={}", parkCode);
                return Result.error("过夜配置保存失败");
            }

        } catch (Exception e) {
            log.error("❌ [保存过夜配置异常] parkCode={}, error={}", parkCode, e.getMessage(), e);
            return Result.error("保存过夜配置失败: " + e.getMessage());
        }
    }

    /**
     * 🆕 手动处理单条违规记录（支持黑名单）
     */
    @PostMapping("/{id}/process")
    @ApiOperation("手动处理违规记录")
    public Result<Boolean> processViolation(
            @ApiParam("违规记录ID") @PathVariable Long id,
            @ApiParam("处理参数") @RequestBody Map<String, Object> processParams,
            HttpServletRequest request) {

        log.info("👨‍💼 [手动处理接口] 接收到处理请求 - ID: {}, 参数: {}", id, processParams);

        try {
            // 从请求体中获取参数
            String processRemark = (String) processParams.get("processRemark");
            String operatorName = (String) processParams.get("operatorName");

            // 黑名单相关参数
            Boolean shouldBlacklist = (Boolean) processParams.get("shouldBlacklist");
            String blacklistType = (String) processParams.get("blacklistType");
            String blacklistReason = (String) processParams.get("blacklistReason");
            Boolean isPermanent = (Boolean) processParams.get("isPermanent");
            String blacklistStartTime = (String) processParams.get("blacklistStartTime");
            String blacklistEndTime = (String) processParams.get("blacklistEndTime");

            // 如果前端没有传递操作员信息，尝试从请求中获取
            if (operatorName == null || operatorName.trim().isEmpty()) {
                operatorName = getCurrentUserId(request);
            }

            if (operatorName == null || "anonymous".equals(operatorName)) {
                log.warn("⚠️ [手动处理接口] 无法获取操作员信息");
                return Result.error("无法获取操作员信息");
            }

            // 调用Service层处理
            boolean result = violationProcessService.manualProcessViolation(id, operatorName, processRemark);

            if (!result) {
                log.warn("⚠️ [手动处理接口] 处理失败 - ID: {}", id);
                return Result.error("处理失败，记录可能不存在或已被处理");
            }

            // 获取违规记录信息（用于日志）
            Violations violation = violationsService.getById(id);
            String plateNumber = violation != null ? violation.getPlateNumber() : "未知";
            String violationType = violation != null ? violation.getViolationType() : "未知";
            
            // 如果需要加入黑名单
            boolean blacklisted = false;
            if (shouldBlacklist != null && shouldBlacklist) {
                log.info("🚫 [手动处理接口] 开始处理黑名单 - ID: {}", id);
                boolean blacklistResult = violationProcessService.addToBlacklist(
                    id, operatorName, blacklistType, blacklistReason,
                    isPermanent, blacklistStartTime, blacklistEndTime
                );

                if (!blacklistResult) {
                    log.warn("⚠️ [手动处理接口] 黑名单处理失败 - ID: {}", id);
                    return Result.error("违规记录已处理，但加入黑名单失败");
                }
                blacklisted = true;
            }

            log.info("✅ [手动处理接口] 处理成功 - ID: {}", id);
            
            // 📝 记录操作日志
            User currentUser = getCurrentUser(request);
            String username = currentUser != null && currentUser.getLoginName() != null 
                            ? currentUser.getLoginName() 
                            : (currentUser != null && currentUser.getUserName() != null 
                                ? currentUser.getUserName() 
                                : operatorName);
            String description;
            if (blacklisted) {
                description = String.format("用户 %s 处理了违规记录并加入黑名单：车牌号 %s，违规类型 %s，处理备注 %s", 
                                          username, plateNumber, violationType, 
                                          processRemark != null ? processRemark : "无");
            } else {
                description = String.format("用户 %s 处理了违规记录：车牌号 %s，违规类型 %s，处理备注 %s", 
                                          username, plateNumber, violationType, 
                                          processRemark != null ? processRemark : "无");
            }
            recordOperation(request, "违规管理", "处理违规记录", description);
            
            return Result.success(true);

        } catch (Exception e) {
            log.error("❌ [手动处理接口] 处理异常 - ID: {}, 错误: {}", id, e.getMessage(), e);
            return Result.error("处理失败：" + e.getMessage());
        }
    }

    /**
     * 🆕 手动批量处理违规记录（支持黑名单）
     */
    @PostMapping("/batch-process")
    @ApiOperation("批量处理违规记录")
    public Result<Map<String, Object>> batchProcessViolations(
            @ApiParam("批量处理参数") @RequestBody Map<String, Object> requestBody,
            HttpServletRequest request) {

        log.info("📋 [批量处理接口] 接收到批量处理请求 - 参数: {}", requestBody);

        try {
            // 解析参数
            @SuppressWarnings("unchecked")
            List<Long> violationIds = (List<Long>) requestBody.get("violationIds");
            String processRemark = (String) requestBody.get("processRemark");
            String operatorName = (String) requestBody.get("operatorName");

            // 黑名单相关参数
            Boolean shouldBlacklist = (Boolean) requestBody.get("shouldBlacklist");
            String blacklistType = (String) requestBody.get("blacklistType");
            String blacklistReason = (String) requestBody.get("blacklistReason");
            Boolean isPermanent = (Boolean) requestBody.get("isPermanent");
            String blacklistStartTime = (String) requestBody.get("blacklistStartTime");
            String blacklistEndTime = (String) requestBody.get("blacklistEndTime");

            if (violationIds == null || violationIds.isEmpty()) {
                return Result.error("违规记录ID列表不能为空");
            }

            // 如果前端没有传递操作员信息，尝试从请求中获取
            if (operatorName == null || operatorName.trim().isEmpty()) {
                operatorName = getCurrentUserId(request);
            }

            if (operatorName == null || "anonymous".equals(operatorName)) {
                return Result.error("无法获取操作员信息");
            }

            log.info("📋 [批量处理接口] 待处理数量: {}, 操作员: {}", violationIds.size(), operatorName);

            // 调用Service层批量处理
            int successCount = violationProcessService.batchProcessViolations(
                    violationIds, operatorName, processRemark);

            // 如果需要加入黑名单
            int blacklistSuccessCount = 0;
            if (shouldBlacklist != null && shouldBlacklist && successCount > 0) {
                log.info("🚫 [批量处理接口] 开始批量处理黑名单 - 数量: {}", violationIds.size());
                for (Long violationId : violationIds) {
                    try {
                        boolean blacklistResult = violationProcessService.addToBlacklist(
                            violationId, operatorName, blacklistType, blacklistReason,
                            isPermanent, blacklistStartTime, blacklistEndTime
                        );
                        if (blacklistResult) {
                            blacklistSuccessCount++;
                        }
                    } catch (Exception e) {
                        log.error("❌ [批量处理接口] 单条黑名单处理失败 - ID: {}, 错误: {}", violationId, e.getMessage());
                    }
                }
                log.info("✅ [批量处理接口] 黑名单处理完成 - 成功: {}/{}", blacklistSuccessCount, violationIds.size());
            }

            // 构建响应结果
            Map<String, Object> result = new HashMap<>();
            result.put("total", violationIds.size());
            result.put("success", successCount);
            result.put("failed", violationIds.size() - successCount);
            if (shouldBlacklist != null && shouldBlacklist) {
                result.put("blacklistSuccess", blacklistSuccessCount);
                result.put("blacklistFailed", violationIds.size() - blacklistSuccessCount);
            }

            log.info("✅ [批量处理接口] 批量处理完成 - 总数: {}, 成功: {}, 失败: {}",
                    violationIds.size(), successCount, violationIds.size() - successCount);

            return Result.success(result);

        } catch (Exception e) {
            log.error("❌ [批量处理接口] 批量处理异常 - 错误: {}", e.getMessage(), e);
            return Result.error("批量处理失败：" + e.getMessage());
        }
    }

    /**
     * 🆕 手动处理指定车牌的所有未处理违规记录（支持黑名单）
     */
    @PostMapping("/process-all-by-plate")
    @ApiOperation("处理指定车牌的所有未处理违规记录")
    public Result<Map<String, Object>> processAllViolationsByPlate(
            @ApiParam("车牌号") @RequestParam String plateNumber,
            @ApiParam("处理参数") @RequestBody Map<String, Object> processParams,
            HttpServletRequest request) {

        log.info("🚗 [处理全部违规接口] 接收到请求 - 车牌: {}, 参数: {}", plateNumber, processParams);

        try {
            // 从请求体中获取参数
            String processRemark = (String) processParams.get("processRemark");
            String operatorName = (String) processParams.get("operatorName");

            // 黑名单相关参数
            Boolean shouldBlacklist = (Boolean) processParams.get("shouldBlacklist");
            String blacklistType = (String) processParams.get("blacklistType");
            String blacklistReason = (String) processParams.get("blacklistReason");
            Boolean isPermanent = (Boolean) processParams.get("isPermanent");
            String blacklistStartTime = (String) processParams.get("blacklistStartTime");
            String blacklistEndTime = (String) processParams.get("blacklistEndTime");

            // 如果前端没有传递操作员信息，尝试从请求中获取
            if (operatorName == null || operatorName.trim().isEmpty()) {
                operatorName = getCurrentUserId(request);
            }

            if (operatorName == null || "anonymous".equals(operatorName)) {
                log.warn("⚠️ [处理全部违规接口] 无法获取操作员信息");
                return Result.error("无法获取操作员信息");
            }

            // 1. 查询该车牌的所有未处理违规记录
            QueryWrapper<Violations> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("plate_number", plateNumber)
                       .eq("process_status", "pending")
                       .orderByDesc("created_at");

            List<Violations> violations = violationsMapper.selectList(queryWrapper);

            if (violations == null || violations.isEmpty()) {
                log.warn("⚠️ [处理全部违规接口] 未找到待处理的违规记录 - 车牌: {}", plateNumber);
                return Result.error("未找到该车牌的待处理违规记录");
            }

            log.info("📋 [处理全部违规接口] 找到 {} 条待处理违规记录", violations.size());

            // 2. 批量处理所有违规记录
            int successCount = 0;
            int failCount = 0;

            for (Violations violation : violations) {
                try {
                    boolean result = violationProcessService.manualProcessViolation(
                        violation.getId(), operatorName, processRemark);
                    if (result) {
                        successCount++;
                    } else {
                        failCount++;
                    }
                } catch (Exception e) {
                    log.error("❌ [处理全部违规接口] 处理单条记录失败 - ID: {}, 错误: {}",
                        violation.getId(), e.getMessage());
                    failCount++;
                }
            }

            log.info("✅ [处理全部违规接口] 处理完成 - 成功: {}, 失败: {}", successCount, failCount);

            // 3. 如果需要加入黑名单（只需要加一次）
            boolean blacklistResult = false;
            if (shouldBlacklist != null && shouldBlacklist && successCount > 0) {
                log.info("🚫 [处理全部违规接口] 开始处理黑名单 - 车牌: {}", plateNumber);

                // 使用第一条违规记录的ID来调用黑名单方法
                Long firstViolationId = violations.get(0).getId();
                blacklistResult = violationProcessService.addToBlacklist(
                    firstViolationId, operatorName, blacklistType, blacklistReason,
                    isPermanent, blacklistStartTime, blacklistEndTime
                );

                if (!blacklistResult) {
                    log.warn("⚠️ [处理全部违规接口] 黑名单处理失败 - 车牌: {}", plateNumber);
                }
            }

            // 4. 构建响应结果
            Map<String, Object> result = new HashMap<>();
            result.put("plateNumber", plateNumber);
            result.put("total", violations.size());
            result.put("success", successCount);
            result.put("failed", failCount);
            result.put("blacklistAdded", blacklistResult);

            log.info("✅ [处理全部违规接口] 处理完成 - 车牌: {}, 总数: {}, 成功: {}, 失败: {}, 黑名单: {}",
                    plateNumber, violations.size(), successCount, failCount, blacklistResult);

            return Result.success(result);

        } catch (Exception e) {
            log.error("❌ [处理全部违规接口] 处理异常 - 车牌: {}, 错误: {}", plateNumber, e.getMessage(), e);
            return Result.error("处理失败：" + e.getMessage());
        }
    }

    // ==================== 🆕 东北林业大学违规阈值配置接口 ====================

    /**
     * 获取东北林业大学违规阈值配置
     * GET /parking/violations/nebu-config
     */
    @GetMapping("/nebu-config")
    public Result<Map<String, Object>> getNebuViolationConfig() {
        log.info("📥 [Controller] 获取东北林业大学违规阈值配置");

        try {
            Map<String, Object> config = violationsService.getNebuViolationConfig();
            log.info("✅ [Controller] 配置获取成功: {}", config);
            return Result.success(config);
        } catch (Exception e) {
            log.error("❌ [Controller] 获取配置失败", e);
            return Result.error("获取配置失败：" + e.getMessage());
        }
    }

    // ==================== 🆕 学院新城拉黑规则配置接口 ====================

    /**
     * 获取学院新城拉黑规则配置
     * GET /parking/violations/college-new-city-config
     */
    @GetMapping("/college-new-city-config")
    public Result<Map<String, Object>> getCollegeNewCityConfig() {
        log.info("📥 [Controller] 获取学院新城拉黑规则配置");

        try {
            // 默认学院新城车场编码，可按需从配置/字典中读取
            String defaultParkCode = "76F1MLQKL";
            Map<String, Object> config = violationsService.getCollegeNewCityConfig(defaultParkCode);
            log.info("✅ [Controller] 学院新城配置获取成功: {}", config);
            return Result.success(config);
        } catch (Exception e) {
            log.error("❌ [Controller] 获取学院新城配置失败", e);
            return Result.error("获取学院新城配置失败：" + e.getMessage());
        }
    }

    /**
     * 保存学院新城拉黑规则配置
     * POST /parking/violations/college-new-city-config
     * 需要参数：parkCode, park_name, blacklistTimeHours, blacklistType(名称或 code|名称), isPermanent,
     * 可选：blacklistValidDays（当 isPermanent=false 时必填）, nightStartHour, nightEndHour
     */
    @PostMapping("/college-new-city-config")
    public Result<Boolean> saveCollegeNewCityConfig(
            @ApiParam("车场编码") @RequestParam String parkCode,
            @ApiParam("车场名称") @RequestParam(name = "park_name") String parkName,
            @ApiParam("过夜判定时长（小时）") @RequestParam Integer blacklistTimeHours,
            @ApiParam("黑名单类型（只要名称，支持 code|name 传入）") @RequestParam String blacklistType,
            @ApiParam("是否永久拉黑") @RequestParam Boolean isPermanent,
            @ApiParam("临时拉黑有效天数") @RequestParam(required = false) Integer blacklistValidDays,
            @ApiParam("凌晨开始小时") @RequestParam(required = false, defaultValue = "0") Integer nightStartHour,
            @ApiParam("凌晨结束小时") @RequestParam(required = false, defaultValue = "7") Integer nightEndHour) {

        // 兼容传入的 “code|name” 形式，仅提取名称
        String blacklistTypeName = parseBlacklistTypeName(blacklistType);

        log.info("💾 [Controller] 保存学院新城配置 - parkCode={}, parkName={}, hours={}, typeName={}, permanent={}, validDays={}, nightStart={}, nightEnd={}",
                parkCode, parkName, blacklistTimeHours, blacklistTypeName, isPermanent, blacklistValidDays, nightStartHour, nightEndHour);

        try {
            // 参数校验
            if (parkName == null || parkName.trim().isEmpty()) {
                return Result.error("park_name 不能为空");
            }
            if (blacklistTimeHours == null || blacklistTimeHours <= 0) {
                return Result.error("过夜判定时长必须大于0小时");
            }
            if (blacklistTypeName == null || blacklistTypeName.trim().isEmpty()) {
                return Result.error("黑名单类型不能为空");
            }
            if (isPermanent == null) {
                return Result.error("是否永久拉黑不能为空");
            }
            if (!isPermanent) {
                if (blacklistValidDays == null || blacklistValidDays < 1 || blacklistValidDays > 365) {
                    return Result.error("临时拉黑有效天数必须在1-365之间");
                }
            } else {
                // 永久拉黑时忽略有效天数
                blacklistValidDays = null;
            }

            boolean result = violationsService.saveCollegeNewCityConfig(
                    parkCode, parkName, blacklistTimeHours, blacklistTypeName,
                    isPermanent, blacklistValidDays, nightStartHour, nightEndHour);

            if (result) {
                log.info("✅ [Controller] 学院新城配置保存成功 - parkCode={}", parkCode);
                return Result.success(true);
            } else {
                log.warn("⚠️ [Controller] 学院新城配置保存失败 - parkCode={}", parkCode);
                return Result.error("保存学院新城配置失败");
            }

        } catch (Exception e) {
            log.error("❌ [Controller] 学院新城配置保存异常 - parkCode={}", parkCode, e);
            return Result.error("保存学院新城配置失败：" + e.getMessage());
        }
    }

    /**
     * 保存东北林业大学违规阈值配置
     * POST /parking/violations/nebu-config?parkName=东北林业大学&maxViolationCount=5&blacklistValidDays=30
     */
    @PostMapping("/nebu-config")
    public Result<Boolean> saveNebuViolationConfig(
            @RequestParam(defaultValue = "东北林业大学") String parkName,
            @RequestParam Integer maxViolationCount,
            @RequestParam String blacklistType,
            @RequestParam Boolean isPermanent,
            @RequestParam(required = false) Integer blacklistValidDays,
            @RequestParam Integer reminderIntervalMinutes) {
       String blacklistTypeName = parseBlacklistTypeName(blacklistType);
        log.info("📥 [Controller] 保存东北林业大学违规阈值配置 - parkName={}, maxCount={}, blacklistType={}, isPermanent={}, validDays={}, reminderIntervalMinutes={}",
                parkName, maxViolationCount, blacklistTypeName, isPermanent, blacklistValidDays,reminderIntervalMinutes);

        try {
            // 参数校验
            if (maxViolationCount == null || maxViolationCount < 3 || maxViolationCount > 10) {
                log.warn("⚠️ [Controller] 参数校验失败 - maxViolationCount={}", maxViolationCount);
                return Result.error("违规阈值必须在3-10之间");
            }

            if (blacklistTypeName == null || blacklistTypeName.trim().isEmpty()) {
                log.warn("⚠️ [Controller] 参数校验失败 - blacklistTypeName不能为空");
                return Result.error("黑名单类型不能为空");
            }

            if (isPermanent == null) {
                log.warn("⚠️ [Controller] 参数校验失败 - isPermanent为空");
                return Result.error("是否永久拉黑不能为空");
            }

            // 如果是临时拉黑，验证有效天数
            if (!isPermanent && (blacklistValidDays == null || blacklistValidDays < 1 || blacklistValidDays > 365)) {
                log.warn("⚠️ [Controller] 参数校验失败 - 临时拉黑有效天数必须在1-365之间, 当前值: {}", blacklistValidDays);
                return Result.error("临时拉黑有效天数必须在1-365之间");
            }
            if (reminderIntervalMinutes == null || reminderIntervalMinutes <= 0) {
                log.warn("⚠️ [Controller] 参数校验失败 - reminderIntervalMinutes={}", reminderIntervalMinutes);
                return Result.error("违规时间间隔不可为0");
            }

            boolean result = violationsService.saveNebuViolationConfig(
                    parkName, maxViolationCount, blacklistTypeName, isPermanent,
                    blacklistValidDays,reminderIntervalMinutes);

            if (result) {
                log.info("✅ [Controller] 配置保存成功 - parkName={}, maxCount={}, blacklistTypeName={}, isPermanent={}, validDays={}",
                        parkName, maxViolationCount, blacklistTypeName, isPermanent, blacklistValidDays);
                return Result.success(true);
            } else {
                log.warn("⚠️ [Controller] 配置保存失败");
                return Result.error("保存配置失败");
            }
        } catch (Exception e) {
            log.error("❌ [Controller] 保存配置异常", e);
            return Result.error("保存配置失败：" + e.getMessage());
        }
    }

    /**
     * 查询车辆距离拉黑阈值还有几次违规
     * GET /parking/violations/threshold-remaining/{plateNumber}
     * @param plateNumber 车牌号
     * @return 包含违规次数、阈值、剩余次数等信息
     */
    @GetMapping("/threshold-remaining/{plateNumber}")
    @ApiOperation("查询车辆距离拉黑阈值还有几次")
    public Result<Map<String, Object>> getThresholdRemaining(
            @PathVariable String plateNumber,
            @RequestParam(defaultValue = "东北林业大学") String parkName) {
        log.info("📊 [Controller] 查询车辆距离拉黑阈值 - plateNumber={}, parkName={}", plateNumber, parkName);

        try {
            // 1. 获取违规阈值配置
            Map<String, Object> config = violationsService.getNebuViolationConfig();
            Integer maxViolationCount = (Integer) config.get("maxViolationCount");

            if (maxViolationCount == null) {
                maxViolationCount = 5; // 默认值
                log.warn("⚠️ [Controller] 未找到违规阈值配置，使用默认值: {}", maxViolationCount);
            }

            // 2. 统计当前未处理的违规次数
            int currentViolationCount = violationsMapper.countUnprocessedByPlate(plateNumber);

            // 3. 计算剩余次数
            int remainingCount = maxViolationCount - (currentViolationCount - 1);

            // 4. 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("plateNumber", plateNumber);
            result.put("currentViolationCount", currentViolationCount);
            result.put("maxViolationCount", maxViolationCount);
            result.put("remainingCount", Math.max(0, remainingCount)); // 确保不为负数
            result.put("willBeBlacklisted", remainingCount <= 0); // 是否会被拉黑
            result.put("blacklistType", config.get("blacklistType"));
            result.put("isPermanent", config.get("isPermanent"));

            log.info("✅ [Controller] 查询成功 - 当前违规: {}, 阈值: {}, 剩余: {}",
                    currentViolationCount, maxViolationCount, remainingCount);

            return Result.success(result);

        } catch (Exception e) {
            log.error("❌ [Controller] 查询失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // ==================== 📝 操作日志记录方法 ====================

    /**
     * 获取当前登录用户
     */
    private User getCurrentUser(HttpServletRequest request) {
        try {
            return TokenUtils.getCurrentUser();
        } catch (Exception e) {
            log.warn("获取当前用户失败", e);
            return null;
        }
    }

    /**
     * 记录操作日志
     * @param request HTTP请求
     * @param module 模块名
     * @param action 操作类型
     * @param description 详细描述
     */
    private void recordOperation(HttpServletRequest request, String module, String action, String description) {
        try {
            User currentUser = getCurrentUser(request);
            String username = currentUser != null && currentUser.getLoginName() != null 
                            ? currentUser.getLoginName() 
                            : (currentUser != null && currentUser.getUserName() != null 
                                ? currentUser.getUserName() 
                                : "未知用户");

            com.parkingmanage.entity.ActivityLog activityLog = new com.parkingmanage.entity.ActivityLog();
            activityLog.setUserId(currentUser != null ? currentUser.getUserId().toString() : "unknown");
            activityLog.setUsername(username);
            activityLog.setModule(module);
            activityLog.setAction(action);
            activityLog.setDescription(description);
            activityLog.setStatus("success");
            activityLog.setCreatedAt(LocalDateTime.now());
            activityLog.setIpAddress(getClientIpAddress(request));
            activityLog.setUserAgent(request.getHeader("User-Agent"));

            activityLogService.save(activityLog);

            log.info("📝 [操作日志] 用户：{}，模块：{}，操作：{}，描述：{}", username, module, action, description);
        } catch (Exception e) {
            log.warn("记录操作日志失败", e);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0];
            }
        }
        return request.getRemoteAddr();
    }

    // ==================== 📊 新增统计图表API ====================

    /**
     * 1. 高频违规车辆Top10统计
     * GET /parking/violations/stats/top-violators?days=30&limit=10
     */
    @GetMapping("/stats/top-violators")
    @ApiOperation("高频违规车辆Top10统计")
    public Result<List<Map<String, Object>>> getTopViolators(
            @ApiParam("统计天数") @RequestParam(defaultValue = "30") Integer days,
            @ApiParam("返回数量") @RequestParam(defaultValue = "10") Integer limit) {
        try {
            log.info("📊 [统计接口] 高频违规车辆Top{} - 近{}天", limit, days);
            
            List<Map<String, Object>> result = violationsMapper.selectTopViolators(days, limit);
            
            log.info("✅ [统计接口] 高频违规车辆查询成功 - 返回{}条", result.size());
            return Result.success(result);
        } catch (Exception e) {
            log.error("❌ [统计接口] 高频违规车辆查询失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 2. 违规记录趋势统计（近N天）
     * GET /parking/violations/stats/trend?days=30
     */
    @GetMapping("/stats/trend")
    @ApiOperation("违规记录趋势统计")
    public Result<List<Map<String, Object>>> getViolationTrend(
            @ApiParam("统计天数") @RequestParam(defaultValue = "30") Integer days) {
        try {
            log.info("📊 [统计接口] 违规记录趋势统计 - 近{}天", days);
            
            List<Map<String, Object>> result = violationsMapper.selectViolationTrend(days);
            
            log.info("✅ [统计接口] 违规记录趋势查询成功 - 返回{}条", result.size());
            return Result.success(result);
        } catch (Exception e) {
            log.error("❌ [统计接口] 违规记录趋势查询失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 3. 违规类型趋势分析（近N天）
     * GET /parking/violations/stats/type-trend?days=30
     */
    @GetMapping("/stats/type-trend")
    @ApiOperation("违规类型趋势分析")
    public Result<List<Map<String, Object>>> getViolationTypeTrend(
            @ApiParam("统计天数") @RequestParam(defaultValue = "30") Integer days) {
        try {
            log.info("📊 [统计接口] 违规类型趋势分析 - 近{}天", days);
            
            List<Map<String, Object>> result = violationsMapper.selectViolationTypeTrend(days);
            
            log.info("✅ [统计接口] 违规类型趋势查询成功 - 返回{}条", result.size());
            return Result.success(result);
        } catch (Exception e) {
            log.error("❌ [统计接口] 违规类型趋势查询失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 4. 各位置违规频次统计
     * GET /parking/violations/stats/location-frequency?days=30
     */
    @GetMapping("/stats/location-frequency")
    @ApiOperation("各位置违规频次统计")
    public Result<List<Map<String, Object>>> getLocationFrequency(
            @ApiParam("统计天数") @RequestParam(defaultValue = "30") Integer days,
            @ApiParam("位置过滤") @RequestParam(required = false) String location) {
        try {
            log.info("📊 [统计接口] 各位置违规频次统计 - 近{}天, 位置过滤: {}", days, location);
            
            List<Map<String, Object>> result = violationsMapper.selectLocationFrequency(days, location);
            
            log.info("✅ [统计接口] 位置违规频次查询成功 - 返回{}条", result.size());
            return Result.success(result);
        } catch (Exception e) {
            log.error("❌ [统计接口] 位置违规频次查询失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 5. 重复违规车辆预警分析
     * GET /parking/violations/stats/repeat-warning?days=30&minCount=3
     */
    @GetMapping("/stats/repeat-warning")
    @ApiOperation("重复违规车辆预警分析")
    public Result<List<Map<String, Object>>> getRepeatViolationWarning(
            @ApiParam("统计天数") @RequestParam(defaultValue = "30") Integer days,
            @ApiParam("最小违规次数") @RequestParam(defaultValue = "3") Integer minCount) {
        try {
            log.info("📊 [统计接口] 重复违规车辆预警 - 近{}天，最小{}次", days, minCount);
            
            List<Map<String, Object>> result = violationsMapper.selectRepeatViolators(days, minCount);
            
            log.info("✅ [统计接口] 重复违规预警查询成功 - 返回{}条", result.size());
            return Result.success(result);
        } catch (Exception e) {
            log.error("❌ [统计接口] 重复违规预警查询失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // ==================== 🆕 非东林车场专用接口（关联vehicle_reservation表）====================

    /**
     * 🆕 非东林车场：分页查询违规记录（关联vehicle_reservation表）
     * 用于万象上东等后台预约车场，通过车牌号和车场名称关联violations表和vehicle_reservation表
     * GET /parking/violations/non-nefu/list
     */
    @GetMapping("/non-nefu/list")
    @ApiOperation("非东林车场：分页查询违规记录（关联vehicle_reservation表）")
    public Result<IPage<Map<String, Object>>> getViolationsWithReservation(
            @ApiParam("页码") @RequestParam(defaultValue = "1") Integer page,
            @ApiParam("页大小") @RequestParam(defaultValue = "20") Integer size,
            @ApiParam("车牌号") @RequestParam(required = false) String plateNumber,
            @ApiParam("状态") @RequestParam(required = false) String status,
            @ApiParam("违规类型") @RequestParam(required = false) String violationType,
            @ApiParam("开始时间") @RequestParam(required = false) String startDate,
            @ApiParam("结束时间") @RequestParam(required = false) String endDate,
            @ApiParam("创建者") @RequestParam(required = false) String created_by,
            @ApiParam("车场名称") @RequestParam(required = false) String community,
            HttpServletRequest request) {
        
        log.info("🆕 [非东林车场] 查询违规记录 - 车牌: {}, 状态: {}, 类型: {}, 车场: {}", 
                plateNumber, status, violationType, community);
        
        // 解析日期参数
        LocalDateTime parsedStartDate = parseDateTime(startDate, true);
        LocalDateTime parsedEndDate = parseDateTime(endDate, false);
        
        // 获取当前用户ID
        String currentUserId = getCurrentUserId(request);
        
        // 创建者过滤
        String createdByFilter = null;
        if (created_by != null && !created_by.trim().isEmpty()) {
            createdByFilter = created_by.trim();
        }
        
        // 车场过滤
        String communityFilter = null;
        if (community != null && !community.trim().isEmpty()) {
            communityFilter = community.trim();
        }
        
        Page<Map<String, Object>> pageParam = new Page<>(page, size);
        
        IPage<Map<String, Object>> result = violationsService.getViolationsWithReservation(
                pageParam, plateNumber, status, violationType, parsedStartDate, parsedEndDate,
                createdByFilter, communityFilter);
        
        log.info("✅ [非东林车场] 查询成功 - 总数: {}", result.getTotal());
        return Result.success(result);
    }

    /**
     * 🆕 非东林车场：创建违规记录
     * 专为非东北林业大学车场设计，支持关联vehicle_reservation表的预约信息
     * POST /parking/violations/non-nefu/create
     */
    @PostMapping("/non-nefu/create")
    @ApiOperation("非东林车场：创建违规记录")
    public Result<Map<String, Object>> createViolationForNonNefu(
            @RequestBody Violations violation, 
            @ApiParam("车场名称") @RequestParam(required = false) String yardName,
            HttpServletRequest request) {
        
        log.info("🆕 [非东林车场] 创建违规记录 - 车牌: {}, 车场: {}, 违规类型: {}", 
                violation.getPlateNumber(), yardName, violation.getViolationType());
        
        // 验证创建者信息
        if (violation.getCreatedBy() == null || violation.getCreatedBy().trim().isEmpty()) {
            log.warn("⚠️ [非东林车场] 创建者信息为空");
            return Result.error("创建者信息不能为空");
        }
        
        // 如果前端没有传yardName，尝试从violation中获取parkName
        String actualYardName = yardName;
        if (actualYardName == null || actualYardName.trim().isEmpty()) {
            actualYardName = violation.getParkName();
        }
        
        // 检查是否为东北林业大学车场（如果是则拒绝）
        if (actualYardName != null && actualYardName.contains("东北林业大学")) {
            log.warn("⚠️ [非东林车场] 东北林业大学车场应使用标准接口创建违规记录");
            return Result.error("东北林业大学车场请使用标准违规创建接口");
        }
        
        // 🔒 防重复提交检测
        if (violation.getPlateNumber() != null && !violation.getPlateNumber().trim().isEmpty()) {
            boolean isDuplicate = violationsService.checkDuplicateViolation(
                violation.getPlateNumber(), 
                violation.getViolationType(),
                60
            );
            if (isDuplicate) {
                log.warn("⚠️ [非东林车场] 重复提交检测 - 车牌: {}, 类型: {}", 
                    violation.getPlateNumber(), violation.getViolationType());
                return Result.error("请勿重复提交，该车辆刚刚已添加过相同类型的违规记录");
            }
        }
        
        boolean result = violationsService.createViolationForNonNefu(violation, actualYardName);
        
        if (result) {
            log.info("✅ [非东林车场] 违规记录创建成功 - ID: {}", violation.getId());
            
            // 记录操作日志
            User currentUser = getCurrentUser(request);
            String username = currentUser != null && currentUser.getLoginName() != null 
                            ? currentUser.getLoginName() 
                            : violation.getCreatedBy();
            String description = String.format("用户 %s 创建了违规记录（非东林车场）：车牌号 %s，违规类型 %s，车场 %s", 
                                              username, violation.getPlateNumber(), 
                                              violation.getViolationType(), actualYardName);
            recordOperation(request, "违规管理", "新增违规记录（非东林）", description);
            
            // 构建返回数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("violationCreated", true);
            responseData.put("id", violation.getId());
            responseData.put("plateNumber", violation.getPlateNumber());
            responseData.put("violationType", violation.getViolationType());
            responseData.put("parkName", actualYardName);
            responseData.put("isNonNefuViolation", true);
            
            return Result.success(responseData);
        } else {
            log.error("❌ [非东林车场] 违规记录创建失败 - 车牌: {}", violation.getPlateNumber());
            return Result.error("创建失败");
        }
    }

    /**
     * 🆕 非东林车场：根据车牌号和车场名称查询vehicle_reservation预约信息
     * GET /parking/violations/non-nefu/reservation-info
     */
    @GetMapping("/non-nefu/reservation-info")
    @ApiOperation("非东林车场：查询vehicle_reservation预约信息")
    public Result<Map<String, Object>> getVehicleReservationInfo(
            @ApiParam("车牌号") @RequestParam String plateNumber,
            @ApiParam("车场名称") @RequestParam String yardName) {
        
        log.info("🔍 [非东林车场] 查询预约信息 - 车牌: {}, 车场: {}", plateNumber, yardName);
        
        if (plateNumber == null || plateNumber.trim().isEmpty()) {
            return Result.error("车牌号不能为空");
        }
        if (yardName == null || yardName.trim().isEmpty()) {
            return Result.error("车场名称不能为空");
        }
        
        Map<String, Object> reservationInfo = violationsService.getVehicleReservationInfo(plateNumber, yardName);
        
        if (reservationInfo != null) {
            log.info("✅ [非东林车场] 找到预约信息 - 通知人: {}", reservationInfo.get("notifierName"));
            return Result.success(reservationInfo);
        } else {
            log.info("ℹ️ [非东林车场] 未找到预约信息");
            return Result.error("未找到该车辆的预约信息");
        }
    }

}
