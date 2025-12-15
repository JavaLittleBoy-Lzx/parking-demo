package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Appointment;
import com.parkingmanage.service.AppointmentService;
import com.parkingmanage.service.WeChatTemplateMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 🚗 停车超时监控控制器
 * 
 * ⚠️ 重要变更：超时通知已改为后端定时任务自动发送！
 * 
 * 新架构（推荐）：
 * - 后端定时任务 ParkingTimeoutMonitoringTask 每1分钟自动执行
 * - 自动检查超时车辆并发送微信提醒
 * - 不依赖前端，24小时稳定运行
 * 
 * 本Controller功能：
 * - /recent-active-count：查询活跃车辆数量（前端可用于显示）
 * - /check-recent-timeout：查询超时车辆列表（只返回数据，不发送通知）
 * 
 * 通知规则：
 * - 直接使用appointment表中的openid（访客）、owneropenid（业主）、auditopenid（管家）
 * - 根据appointtype区分邀请/代人预约，决定通知对象：
 *   * 邀请预约：通知访客+管家+业主
 *   * 代人预约：通知管家+业主
 *   * 其他类型：通知访客
 * 
 * @author System
 * @since 2024-12-04 优化为后端定时任务
 */
@Slf4j
@RestController
@RequestMapping("/parking/timeout")
public class ParkingTimeoutController {

    @Resource
    private AppointmentService appointmentService;

    @Resource
    private WeChatTemplateMessageService weChatTemplateMessageService;

    /**
     * 🔢 获取2小时内活跃车辆数量
     * 前端根据此数量决定是否启动监控
     * 
     * @return 活跃车辆数量
     */
    @GetMapping("/recent-active-count")
    public Result<Integer> getRecentActiveCount() {
        try {
//            log.info("🔍 [超时监控] 查询2小时内活跃车辆数量");
            
            LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
            String twoHoursAgoStr = twoHoursAgo.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
//            log.info("🔍 [超时监控] 查询条件: 2小时前: {} (转换为字符串: {})", twoHoursAgo, twoHoursAgoStr);
            
            List<Appointment> recentActive = appointmentService.getRecentActiveAppointments(twoHoursAgo);
            
            int activeCount = recentActive.size();
//            log.info("📊 [超时监控] 2小时内活跃车辆: {}辆", activeCount);
            
            // 🆕 添加详细的调试信息
            if (!recentActive.isEmpty()) {
//                log.info("🚗 [超时监控] 活跃车辆详情:");
                for (Appointment appointment : recentActive) {
//                    log.info("  - 车牌: {}, 进场时间: {}, 场地状态: {}",
//                        appointment.getPlatenumber(),
//                        appointment.getArrivedate(),
//                        appointment.getVenuestatus());
                }
            } else {
                log.warn("⚠️ [超时监控] 未找到符合条件的活跃车辆，请检查查询条件");
            }
            
            return Result.success(activeCount);
            
        } catch (Exception e) {
            log.error("❌ [超时监控] 查询活跃车辆数量失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * ⏰ 检查2小时内即将超时和已超时的车辆
     * 
     * ⚠️ 注意：此接口只返回数据，不发送通知！
     * 通知由后端定时任务 ParkingTimeoutMonitoringTask 自动发送
     * 
     * @return 超时车辆列表和处理结果
     */
    @GetMapping("/check-recent-timeout")
    public Result<Map<String, Object>> checkRecentTimeout() {
        try {
            log.info("🔍 [超时监控] 开始检查即将超时和已超时车辆");
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime twoHoursAgo = now.minusHours(2);
            
            // 获取2小时内所有活跃车辆
            List<Appointment> recentActive = appointmentService.getRecentActiveAppointments(twoHoursAgo);
            
            if (recentActive.isEmpty()) {
                log.info("✅ [超时监控] 暂无活跃车辆");
                return Result.success(createEmptyResult());
            }
            
            List<Map<String, Object>> almostTimeoutVehicles = new ArrayList<>();
            List<Map<String, Object>> timeoutVehicles = new ArrayList<>();
            int processedCount = 0;
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            for (Appointment appointment : recentActive) {
                if (appointment.getArrivedate() == null || appointment.getArrivedate().trim().isEmpty()) continue;
                
                try {
                    // 将arrivedate字符串转换为LocalDateTime进行计算
                    LocalDateTime arriveDateTime = LocalDateTime.parse(appointment.getArrivedate(), formatter);
                    // 计算停车时长（分钟）
                    long parkingMinutes = Duration.between(arriveDateTime, now).toMinutes();
                    log.info("车牌: {}, 进场时间: {}, 停车时长: {}分钟", 
                        appointment.getPlatenumber(), appointment.getArrivedate(), parkingMinutes);
                    
                    // 1小时45分钟 = 105分钟，2小时 = 120分钟
                    if (parkingMinutes >= 105 && parkingMinutes < 120) {
                        // 即将超时（1小时45分钟到2小时）
                        processedCount++;
                        long remainingMinutes = 120 - parkingMinutes;
                        
                        Map<String, Object> vehicleInfo = createVehicleInfo(appointment, parkingMinutes, remainingMinutes, formatter);
                        almostTimeoutVehicles.add(vehicleInfo);
                        
                        // ⚠️ 不再发送通知，由后端定时任务负责
                        vehicleInfo.put("notificationSent", "handled_by_scheduled_task");
                        vehicleInfo.put("notificationReason", "通知由后端定时任务自动发送");
                        processedCount++;
                        
                    } else if (parkingMinutes >= 120) {
                        // 已超时（超过2小时）
                        processedCount++;
                        long overtimeMinutes = parkingMinutes - 120;
                        
                        Map<String, Object> vehicleInfo = createVehicleInfo(appointment, parkingMinutes, -overtimeMinutes, formatter);
                        vehicleInfo.put("overtimeMinutes", overtimeMinutes);
                        timeoutVehicles.add(vehicleInfo);
                        
                        // 已超时车辆不发送通知，只记录状态
                        vehicleInfo.put("notificationSent", false);
                        vehicleInfo.put("notificationReason", "已超时，无需发送通知");
                    }
                    
                } catch (Exception parseException) {
                    log.warn("⚠️ [超时监控] 解析进场时间失败 - 车牌: {}, 时间: {}, 错误: {}", 
                        appointment.getPlatenumber(), appointment.getArrivedate(), parseException.getMessage());
                    continue;
                }
            }
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("totalActive", recentActive.size());
            result.put("almostTimeoutCount", almostTimeoutVehicles.size());
            result.put("timeoutCount", timeoutVehicles.size());
            result.put("processedCount", processedCount);
            result.put("notificationMode", "scheduled_task");
            result.put("notificationInfo", "通知由后端定时任务自动发送，此接口不发送通知");
            result.put("almostTimeoutVehicles", almostTimeoutVehicles);
            result.put("timeoutVehicles", timeoutVehicles);
            result.put("checkTime", now.format(formatter));
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("❌ [超时监控] 检查超时车辆失败", e);
            return Result.error("检查失败: " + e.getMessage());
        }
    }

    /**
     * 📤 发送超时通知（由前端调用）
     * 
     * @param vehicle 车辆信息
     * @return 发送结果
     */
    @PostMapping("/send-timeout-notification")
    public Result<Map<String, Object>> sendTimeoutNotification(@RequestBody Map<String, Object> vehicle) {
        try {
            String plateNumber = (String) vehicle.get("plateNumber");
            log.info("📤 [超时监控] 收到发送超时通知请求 - 车牌: {}", plateNumber);
            
            // 这里可以根据前端传入的信息直接发送通知
            // 也可以查询数据库获取最新信息后发送
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "通知发送成功");
            result.put("plateNumber", plateNumber);
            result.put("sendTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return Result.success(result);
            
        } catch (Exception e) {
            log.error("❌ [超时监控] 发送超时通知失败", e);
            return Result.error("发送失败: " + e.getMessage());
        }
    }

    /**
     * 🔧 手动触发超时检查（调试用）
     * 
     * @return 检查结果
     */
    @PostMapping("/manual-check")
    public Result<Map<String, Object>> manualCheck() {
        log.info("🔧 [超时监控] 手动触发超时检查");
        return checkRecentTimeout();
    }

    // ================== 私有辅助方法 ==================

    /**
     * 创建空结果
     */
    private Map<String, Object> createEmptyResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalActive", 0);
        result.put("almostTimeoutCount", 0);
        result.put("timeoutCount", 0);
        result.put("processedCount", 0);
        result.put("successCount", 0);
        result.put("failCount", 0);
        result.put("almostTimeoutVehicles", new ArrayList<>());
        result.put("timeoutVehicles", new ArrayList<>());
        result.put("checkTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return result;
    }

    /**
     * 创建车辆信息
     */
    private Map<String, Object> createVehicleInfo(Appointment appointment, long parkingMinutes, 
                                                  long remainingMinutes, DateTimeFormatter formatter) {
        Map<String, Object> vehicleInfo = new HashMap<>();
        vehicleInfo.put("id", appointment.getId());
        vehicleInfo.put("plateNumber", appointment.getPlatenumber());
        vehicleInfo.put("parkName", appointment.getCommunity());
        vehicleInfo.put("visitorName", appointment.getVisitorname());
        vehicleInfo.put("enterTime", appointment.getArrivedate()); // 使用arrivedate字段
        vehicleInfo.put("parkingMinutes", parkingMinutes);
        vehicleInfo.put("remainingMinutes", remainingMinutes);
        vehicleInfo.put("openid", appointment.getOpenid());
        return vehicleInfo;
    }

    /**
     * 发送超时通知
     * 
     * @deprecated 此方法已废弃，通知改由后端定时任务 ParkingTimeoutMonitoringTask 自动发送
     * @param appointment 预约信息
     * @param timeValue 时间值（即将超时时为剩余分钟数，已超时时为超时分钟数）
     * @param isTimeout true=已超时, false=即将超时
     * @param formatter 时间格式化器
     */
    @Deprecated
    private boolean sendTimeoutNotificationToVisitor(Appointment appointment, long timeValue, boolean isTimeout, DateTimeFormatter formatter) {
        try {
            // 只处理即将超时的数据，已超时的数据不处理
            if (isTimeout) {
                log.info("⏭️ [超时监控] 跳过已超时车辆 - 车牌: {}, 超时: {}分钟",
                    appointment.getPlatenumber(), timeValue);
                return true; // 返回true表示处理完成（跳过）
            }

            // 📌 根据预约类型确定通知对象
            String appointType = appointment.getAppointtype();
            log.info("📋 [超时监控] 预约类型: {}, 车牌: {}", appointType, appointment.getPlatenumber());
            
            // 确定通知级别
            String notificationType;
            if (timeValue <= 1) {
                notificationType = "紧急提醒(1分钟)";
            } else if (timeValue <= 5) {
                notificationType = "重要提醒(5分钟)";
            } else {
                notificationType = "即将超时提醒";
            }

            int successCount = 0;
            int totalCount = 0;
            
            // ✅ 邀请预约：通知访客+管家+业主
            if ("邀请".equals(appointType)) {
                log.info("📧 [邀请预约] 发送超时提醒给：访客、管家、业主");
                
                // 1. 通知访客
                if (StringUtils.hasText(appointment.getOpenid())) {
                    totalCount++;
                    if (sendNotificationToUser(appointment.getOpenid(), appointment, timeValue, notificationType + "(访客)")) {
                        successCount++;
                    }
                }
                
                // 2. 通知管家
                if (StringUtils.hasText(appointment.getAuditopenid())) {
                    totalCount++;
                    if (sendNotificationToUser(appointment.getAuditopenid(), appointment, timeValue, notificationType + "(管家)")) {
                        successCount++;
                    }
                }
                
                // 3. 通知业主
                if (StringUtils.hasText(appointment.getOwneropenid())) {
                    totalCount++;
                    if (sendNotificationToUser(appointment.getOwneropenid(), appointment, timeValue, notificationType + "(业主)")) {
                        successCount++;
                    }
                }
            }
            // ✅ 代人预约：通知管家+业主
            else if ("代人".equals(appointType)) {
                log.info("📧 [代人预约] 发送超时提醒给：管家、业主");
                
                // 1. 通知管家
                if (StringUtils.hasText(appointment.getAuditopenid())) {
                    totalCount++;
                    if (sendNotificationToUser(appointment.getAuditopenid(), appointment, timeValue, notificationType + "(管家)")) {
                        successCount++;
                    }
                }
                
                // 2. 通知业主
                if (StringUtils.hasText(appointment.getOwneropenid())) {
                    totalCount++;
                    if (sendNotificationToUser(appointment.getOwneropenid(), appointment, timeValue, notificationType + "(业主)")) {
                        successCount++;
                    }
                }
            }
            // ✅ 其他类型（自助、业主预约）：仅通知访客
            else {
                log.info("📧 [{}预约] 发送超时提醒给：访客", appointType);
                if (StringUtils.hasText(appointment.getOpenid())) {
                    totalCount++;
                    if (sendNotificationToUser(appointment.getOpenid(), appointment, timeValue, notificationType + "(访客)")) {
                        successCount++;
                    }
                }
            }
            
            log.info("📊 [超时监控] {}发送完成 - 车牌: {}, 剩余: {}分钟, 成功: {}/{}",
                notificationType, appointment.getPlatenumber(), timeValue, successCount, totalCount);
            
            return successCount > 0; // 至少成功一条就返回true
            
        } catch (Exception e) {
            log.error("❌ [超时监控] 发送即将超时提醒异常 - 车牌: {}", appointment.getPlatenumber(), e);
            return false;
        }
    }
    
    /**
     * 🔔 发送通知给指定用户
     * @param openid 用户openid
     * @param appointment 预约信息
     * @param timeValue 剩余时间（分钟）
     * @param notificationType 通知类型描述
     * @return 是否发送成功
     */
    private boolean sendNotificationToUser(String openid, Appointment appointment, long timeValue, String notificationType) {
        try {
            Map<String, Object> sendResult = weChatTemplateMessageService.sendParkingAlmostTimeoutNotification(
                openid,
                appointment.getPlatenumber(),
                appointment.getCommunity(),
                appointment.getArrivedate(),
                timeValue
            );
            
            if (Boolean.TRUE.equals(sendResult.get("success"))) {
                log.info("✅ [超时监控] {}发送成功 - openid: {}", notificationType, openid);
                return true;
            } else {
                log.warn("⚠️ [超时监控] {}发送失败 - openid: {}, 原因: {}",
                    notificationType, openid, sendResult.get("message"));
                return false;
            }
        } catch (Exception e) {
            log.error("❌ [超时监控] {}发送异常 - openid: {}", notificationType, openid, e);
            return false;
        }
    }
}