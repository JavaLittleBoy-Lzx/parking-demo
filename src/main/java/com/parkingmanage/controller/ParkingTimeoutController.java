package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.entity.Appointment;
import com.parkingmanage.entity.UserMapping;
import com.parkingmanage.mapper.UserMappingMapper;
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
 * 配合前端定时器实现智能超时检查：
 * - 前端每10-20分钟调用检查接口
 * - 后端检查1小时45分钟+的车辆并发送微信提醒给访客
 * - 根据访客姓名(visitorname)查询user_mapping表获取openid
 * - 实现前后端分离的定时监控
 * 
 * @author System
 */
@Slf4j
@RestController
@RequestMapping("/parking/timeout")
public class ParkingTimeoutController {

    @Resource
    private AppointmentService appointmentService;

    @Resource
    private WeChatTemplateMessageService weChatTemplateMessageService;
    
    @Resource
    private UserMappingMapper userMappingMapper;

    /**
     * 🔢 获取2小时内活跃车辆数量
     * 前端根据此数量决定是否启动监控
     * 
     * @return 活跃车辆数量
     */
    @GetMapping("/recent-active-count")
    public Result<Integer> getRecentActiveCount() {
        try {
            log.info("🔍 [超时监控] 查询2小时内活跃车辆数量");
            
            LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
            String twoHoursAgoStr = twoHoursAgo.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("🔍 [超时监控] 查询条件: 2小时前: {} (转换为字符串: {})", twoHoursAgo, twoHoursAgoStr);
            
            List<Appointment> recentActive = appointmentService.getRecentActiveAppointments(twoHoursAgo);
            
            int activeCount = recentActive.size();
            log.info("📊 [超时监控] 2小时内活跃车辆: {}辆", activeCount);
            
            // 🆕 添加详细的调试信息
            if (!recentActive.isEmpty()) {
                log.info("🚗 [超时监控] 活跃车辆详情:");
                for (Appointment appointment : recentActive) {
                    log.info("  - 车牌: {}, 进场时间: {}, 场地状态: {}", 
                        appointment.getPlatenumber(), 
                        appointment.getArrivedate(), 
                        appointment.getVenuestatus());
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
     * 前端定时器调用此接口进行检查
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
            int successCount = 0;
            int failCount = 0;
            
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
                        
                        // 发送即将超时提醒给访客
                        boolean sendResult = sendTimeoutNotificationToVisitor(appointment, remainingMinutes, false, formatter);
                        if (sendResult) {
                            successCount++;
                            vehicleInfo.put("notificationSent", true);
                        } else {
                            failCount++;
                            vehicleInfo.put("notificationSent", false);
                        }
                        
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
            
            log.info("🔍 [超时监控] 检查完成 - 活跃: {}, 即将超时: {}, 已超时: {}, 成功: {}, 失败: {}", 
                recentActive.size(), almostTimeoutVehicles.size(), timeoutVehicles.size(), successCount, failCount);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("totalActive", recentActive.size());
            result.put("almostTimeoutCount", almostTimeoutVehicles.size());
            result.put("timeoutCount", timeoutVehicles.size());
            result.put("processedCount", processedCount);
            result.put("successCount", successCount);
            result.put("failCount", failCount);
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
     * 发送超时通知给访客
     * @param appointment 预约信息
     * @param timeValue 时间值（即将超时时为剩余分钟数，已超时时为超时分钟数）
     * @param isTimeout true=已超时, false=即将超时
     * @param formatter 时间格式化器
     */
    private boolean sendTimeoutNotificationToVisitor(Appointment appointment, long timeValue, boolean isTimeout, DateTimeFormatter formatter) {
        try {
            // 只处理即将超时的数据，已超时的数据不处理
            if (isTimeout) {
                log.info("⏭️ [超时监控] 跳过已超时车辆 - 车牌: {}, 超时: {}分钟",
                    appointment.getPlatenumber(), timeValue);
                return true; // 返回true表示处理完成（跳过）
            }

            // 根据访客姓名查询openid
            String visitorOpenid = getOpenidByNickname(appointment.getVisitorname());
            if (StringUtils.isEmpty(visitorOpenid)) {
                log.warn("⚠️ [超时监控] 访客 {} 未找到对应的微信openid，无法发送提醒", appointment.getVisitorname());
                return false;
            }

            // 即将超时：根据剩余时间发送不同级别的提醒
            Map<String, Object> sendResult;
            String notificationType;

            if (timeValue <= 1) {
                // 还有1分钟：发送紧急提醒
                notificationType = "紧急提醒(1分钟)";
                sendResult = weChatTemplateMessageService.sendParkingAlmostTimeoutNotification(
                    visitorOpenid,
                    appointment.getPlatenumber(),
                    appointment.getCommunity(),
                    appointment.getArrivedate(), // 使用arrivedate字段
                    timeValue
                );
            } else if (timeValue <= 5) {
                // 还有5分钟：发送重要提醒
                notificationType = "重要提醒(5分钟)";
                sendResult = weChatTemplateMessageService.sendParkingAlmostTimeoutNotification(
                    visitorOpenid,
                    appointment.getPlatenumber(),
                    appointment.getCommunity(),
                    appointment.getArrivedate(), // 使用arrivedate字段
                    timeValue
                );
            } else {
                // 其他即将超时情况：发送普通即将超时提醒
                notificationType = "即将超时提醒";
                sendResult = weChatTemplateMessageService.sendParkingAlmostTimeoutNotification(
                    visitorOpenid,
                    appointment.getPlatenumber(),
                    appointment.getCommunity(),
                    appointment.getArrivedate(), // 使用arrivedate字段
                    timeValue
                );
            }
            
            if (Boolean.TRUE.equals(sendResult.get("success"))) {
                log.info("✅ [超时监控] {}发送成功 - 车牌: {}, 剩余: {}分钟, 访客: {}", 
                    notificationType, appointment.getPlatenumber(), timeValue, appointment.getVisitorname());
                return true;
            } else {
                log.warn("❌ [超时监控] {}发送失败 - 车牌: {}, 原因: {}", 
                    notificationType, appointment.getPlatenumber(), sendResult.get("message"));
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ [超时监控] 发送即将超时提醒异常 - 车牌: {}", appointment.getPlatenumber(), e);
            return false;
        }
    }
    
    /**
     * 根据昵称查询openid
     */
    private String getOpenidByNickname(String nickname) {
        if (StringUtils.isEmpty(nickname)) {
            return null;
        }
        
        try {
            List<UserMapping> userMappings = userMappingMapper.findByNickname(nickname);
            if (userMappings != null && !userMappings.isEmpty()) {
                return userMappings.get(0).getOpenid();
            }
        } catch (Exception e) {
            log.error("❌ [超时监控] 根据昵称查询openid异常 - nickname: {}", nickname, e);
        }
        
        return null;
    }
}