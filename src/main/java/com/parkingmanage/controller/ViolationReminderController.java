package com.parkingmanage.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.entity.ViolationReminder;
import com.parkingmanage.service.ViolationReminderService;
import com.parkingmanage.service.ViolationConfigService;
import com.parkingmanage.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;

/**
 * 违规提醒管理控制器
 * 提供违规提醒记录的查询、处理等功能
 */
@RestController
@RequestMapping("/parking/violationReminders")
@CrossOrigin
public class ViolationReminderController {

    @Autowired
    private ViolationReminderService violationReminderService;

    @Autowired
    private ViolationConfigService violationConfigService;

    /**
     * 获取违规提醒记录列表
     * @param plateNumber 车牌号（可选）
     * @param ownerName 车主姓名（可选）
     * @param violationType 违规类型（可选）
     * @param isProcessed 处理状态（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param parkCode 车场编码（可选）
     * @param current 页码
     * @param size 每页大小
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result getViolationReminders(
            @RequestParam(required = false) String plateNumber,
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) String violationType,
            @RequestParam(required = false) Integer isProcessed,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String parkCode,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size) {
        
        try {
            // 创建分页对象
            Page<ViolationReminder> page = new Page<>(current, size);
            
            // 创建查询条件
            QueryWrapper<ViolationReminder> queryWrapper = new QueryWrapper<>();
            
            if (plateNumber != null && !plateNumber.trim().isEmpty()) {
                queryWrapper.like("plate_number", plateNumber);
            }
            if (ownerName != null && !ownerName.trim().isEmpty()) {
                queryWrapper.like("owner_name", ownerName);
            }
            if (violationType != null && !violationType.trim().isEmpty()) {
                queryWrapper.eq("violation_type", violationType);
            }
            if (isProcessed != null) {
                queryWrapper.eq("is_processed", isProcessed);
            }
            if (parkCode != null && !parkCode.trim().isEmpty()) {
                queryWrapper.eq("park_code", parkCode);
            }
            if (startTime != null && !startTime.trim().isEmpty()) {
                queryWrapper.ge("create_time", startTime);
            }
            if (endTime != null && !endTime.trim().isEmpty()) {
                queryWrapper.le("create_time", endTime);
            }
            
            // 按创建时间倒序排列
            queryWrapper.orderByDesc("create_time");
            
            // 执行分页查询
            IPage<ViolationReminder> result = violationReminderService.page(page, queryWrapper);
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询违规提醒记录失败: " + e.getMessage());
        }
    }

    /**
     * 处理违规提醒记录（标记为已处理）
     * @param id 违规提醒记录ID
     * @param processedBy 处理人
     * @return 处理结果
     */
    @PutMapping("/{id}/process")
    public Result processViolationReminder(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String processedBy = request.get("processedBy");
            if (processedBy == null || processedBy.trim().isEmpty()) {
                processedBy = "管理员";
            }
            
            boolean success = violationReminderService.processViolationReminder(id, processedBy);
            if (success) {
                return Result.success("处理成功");
            } else {
                return Result.error("处理失败，记录不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("处理失败: " + e.getMessage());
        }
    }

    /**
     * 重发违规提醒短信
     * @param id 违规提醒记录ID
     * @return 重发结果
     */
    @PostMapping("/{id}/resend")
    public Result resendViolationReminder(@PathVariable Long id) {
        try {
            // 这里可以调用短信发送服务
            // 暂时返回成功，实际实现需要调用短信服务
            return Result.success("短信重发成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("短信重发失败: " + e.getMessage());
        }
    }

    /**
     * 获取违规提醒统计信息
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param parkCode 车场编码（可选）
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public Result getViolationReminderStatistics(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String parkCode) {
        
        try {
            Map<String, Object> statistics = new HashMap<>();
            
            // 创建查询条件
            QueryWrapper<ViolationReminder> queryWrapper = new QueryWrapper<>();
            if (parkCode != null && !parkCode.trim().isEmpty()) {
                queryWrapper.eq("park_code", parkCode);
            }
            if (startTime != null && !startTime.trim().isEmpty()) {
                queryWrapper.ge("create_time", startTime);
            }
            if (endTime != null && !endTime.trim().isEmpty()) {
                queryWrapper.le("create_time", endTime);
            }
            
            // 总数量
            long total = violationReminderService.count(queryWrapper);
            statistics.put("total", total);
            
            // 未处理数量
            QueryWrapper<ViolationReminder> unprocessedWrapper = queryWrapper.clone();
            unprocessedWrapper.eq("is_processed", 0);
            long unprocessed = violationReminderService.count(unprocessedWrapper);
            statistics.put("unprocessed", unprocessed);
            
            // 已处理数量
            QueryWrapper<ViolationReminder> processedWrapper = queryWrapper.clone();
            processedWrapper.eq("is_processed", 1);
            long processed = violationReminderService.count(processedWrapper);
            statistics.put("processed", processed);
            
            // 今日新增数量
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            QueryWrapper<ViolationReminder> todayWrapper = queryWrapper.clone();
            todayWrapper.ge("create_time", today + " 00:00:00");
            todayWrapper.le("create_time", today + " 23:59:59");
            long todayCount = violationReminderService.count(todayWrapper);
            statistics.put("today", todayCount);
            
            return Result.success(statistics);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 根据车牌号获取违规提醒历史记录
     * @param plateNumber 车牌号
     * @param parkCode 车场编码（可选）
     * @return 历史记录
     */
    @GetMapping("/history/{plateNumber}")
    public Result getViolationReminderHistory(@PathVariable String plateNumber, 
                                            @RequestParam(required = false) String parkCode) {
        try {
            QueryWrapper<ViolationReminder> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("plate_number", plateNumber);
            if (parkCode != null && !parkCode.trim().isEmpty()) {
                queryWrapper.eq("park_code", parkCode);
            }
            queryWrapper.orderByDesc("create_time");
            
            return Result.success(violationReminderService.list(queryWrapper));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取历史记录失败: " + e.getMessage());
        }
    }

    /**
     * 创建违规提醒记录
     * @param reminderData 提醒数据
     * @return 创建结果
     */
    @PostMapping("/add")
    public Result createViolationReminder(@RequestBody ViolationReminder reminderData) {
        try {
            // 设置创建时间
            reminderData.setCreateTime(LocalDateTime.now());
            reminderData.setReminderTime(LocalDateTime.now());
            
            // 如果未设置处理状态，默认为未处理
            if (reminderData.getIsProcessed() == null) {
                reminderData.setIsProcessed(0);
            }
            
            boolean success = violationReminderService.save(reminderData);
            if (success) {
                return Result.success(reminderData);
            } else {
                return Result.error("创建违规提醒记录失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("创建违规提醒记录失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否需要发送违规提醒
     * @param plateNumber 车牌号
     * @return 检查结果
     */
    @GetMapping("/check-reminder")
    public Result checkReminderNeeded(@RequestParam String plateNumber) {
        try {
            // 查询该车牌号是否有未处理的违规提醒
            QueryWrapper<ViolationReminder> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("plate_number", plateNumber);
            queryWrapper.eq("is_processed", 0);
            
            long count = violationReminderService.count(queryWrapper);
            
            Map<String, Object> result = new HashMap<>();
            result.put("plateNumber", plateNumber);
            result.put("needsReminder", count > 0);
            result.put("unprocessedCount", count);
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("检查提醒需求失败: " + e.getMessage());
        }
    }

    /**
     * 根据车牌号查询未处理的违规提醒
     * @param plateNumber 车牌号
     * @param page 页码
     * @param size 每页大小
     * @return 未处理的提醒列表
     */
    @GetMapping("/unprocessed")
    public Result getUnprocessedRemindersByPlate(
            @RequestParam String plateNumber,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            // 创建分页对象
            Page<ViolationReminder> pageObj = new Page<>(page, size);
            
            // 创建查询条件
            QueryWrapper<ViolationReminder> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("plate_number", plateNumber);
            queryWrapper.eq("is_processed", 0);
            queryWrapper.orderByDesc("create_time");
            
            // 执行分页查询
            IPage<ViolationReminder> result = violationReminderService.page(pageObj, queryWrapper);
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询未处理提醒失败: " + e.getMessage());
        }
    }

    /**
     * 获取违规提醒相关设置（如最小发送间隔分钟数）
     */
    @GetMapping("/settings")
    public Result getReminderSettings() {
        try {
            int minutes = violationConfigService.getReminderIntervalMinutes("东北林业大学", 0);
            Map<String, Object> data = new HashMap<>();
            data.put("reminderIntervalMinutes", minutes);
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取提醒设置失败: " + e.getMessage());
        }
    }

    /**
     * 更新违规提醒设置（最小发送间隔分钟数）
     */
    @PutMapping("/settings")
    public Result updateReminderSettings(@RequestBody Map<String, Object> request) {
        try {
            Object minutesObj = request.get("reminderIntervalMinutes");
            if (minutesObj == null) {
                return Result.error("reminderIntervalMinutes 不能为空");
            }
            int minutes;
            try {
                minutes = Integer.parseInt(String.valueOf(minutesObj));
            } catch (NumberFormatException nfe) {
                return Result.error("reminderIntervalMinutes 必须为数字");
            }
            if (minutes <= 0 || minutes > 7 * 24 * 60) {
                return Result.error("reminderIntervalMinutes 超出合法范围");
            }

            boolean ok = violationConfigService.updateReminderIntervalMinutes("GLOBAL", minutes, "system");
            if (ok) {
                Map<String, Object> data = new HashMap<>();
                data.put("reminderIntervalMinutes", minutes);
                return Result.success(data);
            }
            return Result.error("更新提醒设置失败");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新提醒设置失败: " + e.getMessage());
        }
    }

    /**
     * 判断某车牌是否已超过最小发送间隔
     */
    @GetMapping("/check-interval")
    public Result checkReminderInterval(@RequestParam String plateNumber) {
        try {
            if (plateNumber == null || plateNumber.trim().isEmpty()) {
                return Result.error("plateNumber 不能为空");
            }

            int minInterval = violationConfigService.getReminderIntervalMinutes("GLOBAL", 30);

            QueryWrapper<ViolationReminder> qw = new QueryWrapper<>();
            qw.eq("plate_number", plateNumber);
            qw.orderByDesc("reminder_time");
            qw.last("limit 1");
            List<ViolationReminder> lastList = violationReminderService.list(qw);

            Integer minutesSinceLast = null;
            boolean canSend = true;

            if (!lastList.isEmpty()) {
                ViolationReminder last = lastList.get(0);
                LocalDateTime lastTime = last.getReminderTime() != null ? last.getReminderTime() : last.getCreateTime();
                if (lastTime != null) {
                    long diffMinutes = Duration.between(lastTime, LocalDateTime.now()).toMinutes();
                    minutesSinceLast = (int) Math.max(diffMinutes, 0);
                    canSend = minutesSinceLast >= minInterval;
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("plateNumber", plateNumber);
            data.put("minIntervalMinutes", minInterval);
            data.put("minutesSinceLast", minutesSinceLast);
            data.put("canSend", canSend);
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("检查发送间隔失败: " + e.getMessage());
        }
    }

    /**
     * 标记所有未处理的提醒为已处理
     * @param request 请求参数
     * @return 处理结果
     */
    @PutMapping("/mark-all-processed")
    public Result markAllRemindersAsProcessed(@RequestBody Map<String, Object> request) {
        try {
            String plateNumber = (String) request.get("plateNumber");
            String processedBy = (String) request.get("processedBy");
            String processedTimeStr = (String) request.get("processedTime");
            
            if (plateNumber == null || plateNumber.trim().isEmpty()) {
                return Result.error("车牌号不能为空");
            }
            
            if (processedBy == null || processedBy.trim().isEmpty()) {
                processedBy = "管理员";
            }
            
            LocalDateTime processedTime = LocalDateTime.now();
            if (processedTimeStr != null && !processedTimeStr.trim().isEmpty()) {
                try {
                    processedTime = LocalDateTime.parse(processedTimeStr);
                } catch (Exception e) {
                    // 如果解析失败，使用当前时间
                }
            }
            
            // 查询该车牌号所有未处理的提醒
            QueryWrapper<ViolationReminder> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("plate_number", plateNumber);
            queryWrapper.eq("is_processed", 0);
            
            List<ViolationReminder> unprocessedReminders = violationReminderService.list(queryWrapper);
            
            if (unprocessedReminders.isEmpty()) {
                return Result.success("没有未处理的提醒记录");
            }
            
            // 批量更新处理状态
            for (ViolationReminder reminder : unprocessedReminders) {
                reminder.setIsProcessed(1);
                reminder.setProcessedBy(processedBy);
                reminder.setProcessedTime(processedTime);
            }
            
            boolean success = violationReminderService.updateBatchById(unprocessedReminders);
            if (success) {
                Map<String, Object> result = new HashMap<>();
                result.put("processedCount", unprocessedReminders.size());
                result.put("plateNumber", plateNumber);
                return Result.success(result);
            } else {
                return Result.error("批量处理失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("批量处理失败: " + e.getMessage());
        }
    }

    /**
     * 发送违规提醒短信
     * @param request 请求参数
     * @return 发送结果
     */
    @PostMapping("/send-reminder-sms")
    public Result sendViolationReminderSms(@RequestBody Map<String, Object> request) {
        try {
            String phoneNumber = (String) request.get("phoneNumber");
            @SuppressWarnings("unchecked")
            Map<String, Object> templateData = (Map<String, Object>) request.get("templateData");
            
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return Result.error("手机号不能为空");
            }
            
            if (templateData == null) {
                return Result.error("模板数据不能为空");
            }
            
            // 这里可以调用短信发送服务
            // 暂时返回成功，实际实现需要调用短信服务
            Map<String, Object> result = new HashMap<>();
            result.put("phoneNumber", phoneNumber);
            result.put("templateData", templateData);
            result.put("message", "违规提醒短信发送成功");
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发送短信失败: " + e.getMessage());
        }
    }

    /**
     * 发送违规短信
     * @param request 请求参数
     * @return 发送结果
     */
    @PostMapping("/send-violation-sms")
    public Result sendViolationSms(@RequestBody Map<String, Object> request) {
        try {
            String phoneNumber = (String) request.get("phoneNumber");
            @SuppressWarnings("unchecked")
            Map<String, Object> templateData = (Map<String, Object>) request.get("templateData");
            
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                return Result.error("手机号不能为空");
            }
            
            if (templateData == null) {
                return Result.error("模板数据不能为空");
            }
            
            // 这里可以调用短信发送服务
            // 暂时返回成功，实际实现需要调用短信服务
            Map<String, Object> result = new HashMap<>();
            result.put("phoneNumber", phoneNumber);
            result.put("templateData", templateData);
            result.put("message", "违规短信发送成功");
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发送短信失败: " + e.getMessage());
        }
    }

    // ==================== 📊 违规提醒统计API ====================

    /**
     * 8. 违规记录与提醒发送关联分析
     * GET /parking/violationReminders/stats/correlation?days=30
     */
    @GetMapping("/stats/correlation")
    public Result<List<Map<String, Object>>> getViolationReminderCorrelation(
            @RequestParam(defaultValue = "30") Integer days) {
        try {
            // 调用Service层获取关联分析数据
            List<Map<String, Object>> result = violationReminderService.getCorrelationAnalysis(days);
            
            return Result.success(result);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败: " + e.getMessage());
        }
    }
}
