package com.parkingmanage.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.controller.VehicleReservationController;
import com.parkingmanage.entity.Appointment;
import com.parkingmanage.entity.MonthTick;
import com.parkingmanage.entity.MonthlyTicketTimeoutConfig;
import com.parkingmanage.entity.Patrol;
import com.parkingmanage.service.AppointmentService;
import com.parkingmanage.service.MonthTicketService;
import com.parkingmanage.service.MonthlyTicketTimeoutConfigService;
import com.parkingmanage.service.PatrolService;
import com.parkingmanage.service.ViolationsService;
import com.parkingmanage.service.WeChatTemplateMessageService;
import com.parkingmanage.common.config.AIKEConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 停车超时监控定时任务
 * 
 * 功能说明：
 * - 自动监控2小时内的活跃车辆
 * - 在停车1:45、1:55、1:59时发送超时提醒
 * - 根据预约类型（邀请/代人/其他）智能分发通知给访客、管家、业主
 * 
 * 执行频率：每1分钟执行一次
 * 
 * 优势：
 * 1. 后端自动执行，不依赖前端
 * 2. 稳定可靠，不受用户操作影响
 * 3. 节省用户流量和电量
 * 4. 集中管理，便于监控和调试
 * 
 * @author System
 * @since 2024-12-04
 */
@Slf4j
@Component
public class ParkingTimeoutMonitoringTask {

    @Resource
    private AppointmentService appointmentService;

    @Resource
    private WeChatTemplateMessageService weChatTemplateMessageService;
    
    @Resource
    private PatrolService patrolService;
    
    @Resource
    private VehicleReservationController vehicleReservationController;
    
    @Resource
    private ViolationsService violationsService;
    
    @Resource
    private MonthTicketService monthTicketService;
    
    @Resource
    private MonthlyTicketTimeoutConfigService monthlyTicketTimeoutConfigService;
    
    @Resource
    private AIKEConfig aikeConfig;
    
    /**
     * 默认推送时间段配置
     * 如果数据库中未配置，则使用默认值：23:00-06:00
     */
    private static final String DEFAULT_NOTIFICATION_START_TIME = "23:00";
    private static final String DEFAULT_NOTIFICATION_END_TIME = "06:00";
    
    /**
     * 发送记录缓存：避免重复发送
     * Key格式：appointmentId_notifyPoint（如："123_15" 表示预约ID 123 在15分钟时间点）
     * Value：最后发送时间
     */
    private final Map<String, LocalDateTime> notificationSentCache = new ConcurrentHashMap<>();
    
    /**
     * 万象上东拉黑缓存：避免同一车辆短时间内重复拉黑
     * Key：车牌号，Value：最后拉黑时间
     */
    private final Map<String, LocalDateTime> wanXiangBlacklistCache = new ConcurrentHashMap<>();

    /**
     * 🔥 【优化版】定时检查停车超时情况
     * 
     * cron表达式：0 * * * * ?
     * 含义：每分钟的第0秒执行（即每1分钟执行一次）
     * 
     * 优化后的执行逻辑：
     * 1. 分别精准查询30/60/90/105分钟前进场的车辆（不再查询所有2小时内的车辆）
     * 2. 直接发送通知，无需计算停车时长
     * 3. 根据预约类型智能分发通知
     * 
     * 优势：
     * - 查询效率提升：只查询需要通知的车辆
     * - 计算量减少：无需逐个计算停车时长
     * - 避免重复：同一辆车不会被反复查询
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkParkingTimeout() {
        try {
            log.info("🔥 [定时任务-优化版] 停车超时监控开始执行");
            
            // 清理30分钟前的过期缓存记录
            cleanExpiredCache();
            
            LocalDateTime now = LocalDateTime.now();
            int totalNotified = 0;
            
            // 🔥 【优化】分别查询4个时间点的车辆，避免查询所有2小时内的车辆
            // 1. 查询30分钟前进场的车辆（29-31分钟范围）
            totalNotified += checkAndNotifyByTimePoint(now, 30, "retention", "车主滞留通知");
            
            // 2. 查询60分钟前进场的车辆（59-61分钟范围）
            totalNotified += checkAndNotifyByTimePoint(now, 60, "retention", "车主滞留通知");
            
            // 3. 查询90分钟前进场的车辆（89-91分钟范围）
            totalNotified += checkAndNotifyByTimePoint(now, 90, "timeout", "超时提醒(剩余30分钟)");
            
            // 4. 查询105分钟前进场的车辆（104-106分钟范围）
            totalNotified += checkAndNotifyByTimePoint(now, 105, "timeout", "超时提醒(剩余15分钟)");
            
            // 5. 🚫 查询120分钟前进场的车辆（119-121分钟范围）→ 触发拉黑
            int blacklistedCount = checkAndBlacklistByTimePoint(now, 120);
            
            log.info("✅ [定时任务-优化版] 检查完成 - 发送通知: {}条, 拉黑: {}辆", totalNotified, blacklistedCount);
            
            // 🆕 检查万象上东VIP月票车拉黑条件（保留原有逻辑）
            checkWanXiangVipBlacklist();
            
        } catch (Exception e) {
            log.error("❌ [定时任务-优化版] 停车超时监控执行异常", e);
        }
    }
    
    /**
     * 🔥 【核心方法】检查并通知特定时间点的车辆
     * 
     * @param now 当前时间
     * @param minutesAgo 多少分钟前进场（30/60/90/105）
     * @param notifyType 通知类型：retention=滞留通知，timeout=超时提醒
     * @param notifyDesc 通知描述
     * @return 发送成功的通知数量
     */
    private int checkAndNotifyByTimePoint(LocalDateTime now, int minutesAgo, String notifyType, String notifyDesc) {
        try {
            // 1. 计算时间范围（±1分钟容差）
            LocalDateTime startTime = now.minusMinutes(minutesAgo + 1);  // 例如：31分钟前
            LocalDateTime endTime = now.minusMinutes(minutesAgo - 1);    // 例如：29分钟前
            
            // 2. 精准查询该时间段进场的车辆
            List<Appointment> appointments = appointmentService.getActiveAppointmentsByTimeRange(startTime, endTime);
            
            if (appointments.isEmpty()) {
                log.debug("⏭️ [{}分钟通知] 无符合条件的车辆", minutesAgo);
                return 0;
            }
            
            log.info("📊 [{}分钟通知] 发现 {} 辆车辆，准备发送{}", minutesAgo, appointments.size(), notifyDesc);
            
            // 3. 遍历车辆，检查并发送通知
            int successCount = 0;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            for (Appointment appointment : appointments) {
                try {
                    // 解析进场时间
                    LocalDateTime arriveDateTime = LocalDateTime.parse(appointment.getArrivedate(), formatter);
                    
                    // 计算实际停车时长（用于日志和通知内容）
                    long parkingMinutes = Duration.between(arriveDateTime, now).toMinutes();
                    
                    // 🆕 智能推送时间判断：只对万象上东车场启用夜间限制
                    if ("万象上东".equals(appointment.getCommunity())) {
                        String parkCode = "2KST9MNP";  // 万象上东车场编码
                        // 获取实际使用的时间段配置（用于日志显示）
                        String[] timeRange = getNotificationTimeRange(parkCode);
                        boolean isInNightTime = isInNotificationTimeByDuration(arriveDateTime, parkingMinutes, parkCode);
                        if (!isInNightTime) {
                            log.info("⏭️ [{}分钟通知-万象上东] 车牌 {} 当前不在夜间推送时段（{}~{}），白天不算违规，跳过通知", 
                                minutesAgo, appointment.getPlatenumber(), timeRange[0], timeRange[1]);
                            continue;
                        }
                        log.info("✅ [{}分钟通知-万象上东] 车牌 {} 在夜间时段（{}~{}），继续发送通知", 
                            minutesAgo, appointment.getPlatenumber(), timeRange[0], timeRange[1]);
                    }
                    
                    // 检查是否已发送过（避免重复）
                    String cacheKey = generateCacheKey(appointment.getId(), minutesAgo);
                    if (isAlreadySent(cacheKey)) {
                        log.info("⏭️ [{}分钟通知] 车牌 {} 已发送过，跳过", minutesAgo, appointment.getPlatenumber());
                        continue;
                    }
                    
                    // 发送通知
                    log.info("🚀 [{}分钟通知] 开始发送 - 车牌: {}, 车场: {}, 类型: {}, 实际停车: {}分钟, 通知时间点: {}分钟", 
                        minutesAgo, appointment.getPlatenumber(), appointment.getCommunity(), notifyType, parkingMinutes, minutesAgo);
                    
                    boolean sent;
                    if ("retention".equals(notifyType)) {
                        // 滞留通知：发送给车主，使用目标时间点（30/60）
                        sent = sendRetentionNotification(appointment, minutesAgo);
                    } else {
                        // 超时提醒：发送给巡检员、管家，使用目标剩余时间（30/15）
                        long remainingMinutes = 120 - minutesAgo;  // 剩余时间（基于目标时间点）
                        sent = sendTimeoutNotification(appointment, remainingMinutes);
                    }
                    
                    if (sent) {
                        markAsSent(cacheKey);
                        successCount++;
                        log.info("✅ [{}分钟通知] 车牌 {} 发送成功 - 通知时间点: {}分钟", 
                            minutesAgo, appointment.getPlatenumber(), minutesAgo);
                    } else {
                        log.warn("❌ [{}分钟通知] 车牌 {} 发送失败 - 车场: {}, 类型: {}, 停车时长: {}分钟，请检查原因", 
                            minutesAgo, appointment.getPlatenumber(), appointment.getCommunity(), notifyType, parkingMinutes);
                    }
                    
                } catch (Exception e) {
                    log.warn("⚠️ [{}分钟通知] 处理车辆异常 - 车牌: {}, 错误: {}", 
                        minutesAgo, appointment.getPlatenumber(), e.getMessage());
                }
            }
            
            log.info("📊 [{}分钟通知] 处理完成 - 总计: {}辆, 成功发送: {}条", minutesAgo, appointments.size(), successCount);
            return successCount;
            
        } catch (Exception e) {
            log.error("❌ [{}分钟通知] 执行异常", minutesAgo, e);
            return 0;
        }
    }
    
    /**
     * 🔥 【修正版】发送超时通知给巡检员和管家
     * 
     * 90分钟、105分钟的超时通知统一发给：
     * 1. 所有值班巡检员
     * 2. 管家（如果有auditopenid）
     * 
     * 不再根据预约类型区分发送对象
     * 
     * @param appointment 预约信息
     * @param remainingMinutes 剩余时间（分钟）
     * @return 是否至少发送成功一条
     */
    private boolean sendTimeoutNotification(Appointment appointment, long remainingMinutes) {
        try {
            String notificationType = String.format("超时提醒(剩余%d分钟)", remainingMinutes);
            
            log.info("📧 [超时通知-巡检员&管家] 车牌: {}, 车场: {}, 剩余: {}分钟", 
                appointment.getPlatenumber(), appointment.getCommunity(), remainingMinutes);
            
            int successCount = 0;
            int totalCount = 0;
            
            // 1️⃣ 发送给管家（如果有auditopenid）
            if (StringUtils.hasText(appointment.getAuditopenid())) {
                totalCount++;
                log.info("📧 [超时通知] 准备发送给管家 - openid: {}", appointment.getAuditopenid());
                if (sendNotificationToUser(appointment.getAuditopenid(), appointment, remainingMinutes, 
                    notificationType + "(管家)")) {
                    successCount++;
                }
            } else {
                log.debug("⏭️ [超时通知] 无管家openid，跳过管家通知");
            }
            
            // 2️⃣ 查询并发送给所有值班巡检员
            if (StringUtils.hasText(appointment.getCommunity())) {
                List<Patrol> onDutyPatrols = patrolService.getOnDutyPatrolsByCommunity(appointment.getCommunity());
                
                if (!onDutyPatrols.isEmpty()) {
                    log.info("📋 [超时通知] 向 {} 位值班巡检员发送通知 - 车场: {}", 
                        onDutyPatrols.size(), appointment.getCommunity());
                    
                    for (Patrol patrol : onDutyPatrols) {
                        if (StringUtils.hasText(patrol.getOpenid())) {
                            totalCount++;
                            log.info("📧 [超时通知] 准备发送给巡检员 - 姓名: {}, openid: {}", 
                                patrol.getUsername(), patrol.getOpenid());
                            if (sendNotificationToUser(patrol.getOpenid(), appointment, remainingMinutes, 
                                notificationType + "(巡检员:" + patrol.getUsername() + ")")) {
                                successCount++;
                            }
                        }
                    }
                } else {
                    log.warn("⚠️ [超时通知] 车场 {} 当前无值班巡检员", appointment.getCommunity());
                }
            }
            
            log.info("📊 [超时通知-巡检员&管家] 发送完成 - 车牌: {}, 成功: {}/{}", 
                appointment.getPlatenumber(), successCount, totalCount);
            
            if (totalCount == 0) {
                log.warn("⚠️ [超时通知] 没有可发送的目标用户（无管家和巡检员） - 车牌: {}", 
                    appointment.getPlatenumber());
            }
            
            return successCount > 0;
            
        } catch (Exception e) {
            log.error("❌ [超时通知-巡检员&管家] 异常 - 车牌: {}", appointment.getPlatenumber(), e);
            return false;
        }
    }
    
    /**
     * 发送通知给指定用户
     * 
     * @param openid 用户openid
     * @param appointment 预约信息
     * @param remainingMinutes 剩余时间（分钟）
     * @param notificationType 通知类型描述
     * @return 是否发送成功
     */
    private boolean sendNotificationToUser(String openid, Appointment appointment, long remainingMinutes, String notificationType) {
        try {
            // 🔍 添加详细日志诊断进场时间问题
            log.info("📧 [通知准备] 车牌: {}, 进场时间(arrivedate): {}, 预约时间: {}, 车辆状态: {}, openid: {}", 
                appointment.getPlatenumber(), 
                appointment.getArrivedate(),
                appointment.getRecorddate(),
                appointment.getVenuestatus(),
                openid);
            
            java.util.Map<String, Object> sendResult = weChatTemplateMessageService.sendParkingAlmostTimeoutNotification(
                openid,
                appointment.getPlatenumber(),
                appointment.getCommunity(),
                appointment.getArrivedate(),
                remainingMinutes
            );
            
            if (Boolean.TRUE.equals(sendResult.get("success"))) {
                log.info("✅ [定时任务] {}发送成功 - openid: {}", notificationType, openid);
                return true;
            } else {
                log.warn("⚠️ [定时任务] {}发送失败 - openid: {}, 原因: {}",
                    notificationType, openid, sendResult.get("message"));
                return false;
            }
        } catch (Exception e) {
            log.error("❌ [定时任务] {}发送异常 - openid: {}", notificationType, openid, e);
            return false;
        }
    }
    
    /**
     * 🚫 【拉黑方法】检查并拉黑超过120分钟的车辆
     * 
     * @param now 当前时间
     * @param minutesAgo 多少分钟前进场（120）
     * @return 拉黑车辆数量
     */
    private int checkAndBlacklistByTimePoint(LocalDateTime now, int minutesAgo) {
        int blacklistedCount = 0;
        
        try {
            // 1. 计算查询时间范围（±1分钟）
            LocalDateTime targetTime = now.minusMinutes(minutesAgo);
            LocalDateTime startTime = targetTime.minusMinutes(1);
            LocalDateTime endTime = targetTime.plusMinutes(1);
            
            log.info("🚫 [120分钟拉黑] 开始检查 - 时间范围: {} ~ {}", startTime, endTime);
            
            // 2. 查询指定时间范围内进场的车辆
            List<Appointment> appointments = appointmentService.getActiveAppointmentsByTimeRange(
                startTime, endTime
            );
            
            if (appointments.isEmpty()) {
                log.info("✅ [120分钟拉黑] 无超时车辆");
                return 0;
            }
            
            log.info("📊 [120分钟拉黑] 查询到 {} 辆超时车辆", appointments.size());
            
            // 3. 获取配置中规定的月票名称列表
            List<String> allowedTicketTypes = getAllowedTicketTypesForBlacklist();
            if (allowedTicketTypes == null || allowedTicketTypes.isEmpty()) {
                log.warn("⚠️ [120分钟拉黑] 未配置月票名称列表，跳过拉黑");
                return 0;
            }
            
            log.info("📋 [120分钟拉黑] 规定的月票类型: {}", allowedTicketTypes);
            
            // 4. 遍历每个超时车辆
            for (Appointment appointment : appointments) {
                try {
                    String plateNumber = appointment.getPlatenumber();
                    String parkName = appointment.getCommunity();
                    
                    log.info("🔎 [120分钟拉黑] 开始检查车牌: {}, 车场: {}", plateNumber, parkName);
                    
                    // 5. 查询该车牌在month_tick表中的所有记录
                    // 注意：car_no字段可能包含多个车牌，用逗号分隔，如"黑ABY138,京KC9090,湘AFE6876"
                    List<MonthTick> monthTickets = monthTicketService.lambdaQuery()
                        .like(MonthTick::getCarNo, plateNumber)  // 使用like模糊匹配
                        .eq(MonthTick::getValidStatus, 1)  // 有效状态
                        .eq(MonthTick::getIsFrozen, 0)     // 未冻结
                        .list();
                    
                    // 二次过滤：精确匹配车牌号（避免"京A123"匹配到"京A1234"）
                    monthTickets = monthTickets.stream()
                        .filter(ticket -> {
                            String carNo = ticket.getCarNo();
                            if (carNo == null) return false;
                            // 分割车牌号并精确匹配
                            String[] plates = carNo.split(",");
                            for (String plate : plates) {
                                if (plate.trim().equals(plateNumber)) {
                                    return true;
                                }
                            }
                            return false;
                        })
                        .collect(java.util.stream.Collectors.toList());
                    
                    if (monthTickets.isEmpty()) {
                        log.info("⏭️ [120分钟拉黑] 车牌 {} 不在月票表中，跳过", plateNumber);
                        continue;
                    }
                    
                    log.info("🔍 [120分钟拉黑] 车牌 {} 在月票表中有 {} 条记录", plateNumber, monthTickets.size());
                    
                    // 6. 判断是否有符合条件的月票类型
                    boolean hasMatchingTicket = false;
                    StringBuilder ticketNamesLog = new StringBuilder();
                    for (MonthTick ticket : monthTickets) {
                        String ticketName = ticket.getTicketName();
                        ticketNamesLog.append("[").append(ticketName).append("] ");
                        
                        if (allowedTicketTypes.contains(ticketName)) {
                            hasMatchingTicket = true;
                            log.info("✅ [120分钟拉黑] 车牌 {} 的月票类型 \"{}\" 在规定列表中", plateNumber, ticketName);
                            break;
                        }
                    }
                    
                    if (!hasMatchingTicket) {
                        log.info("⏭️ [120分钟拉黑] 车牌 {} 的月票类型不在规定列表中，跳过 - 实际类型: {}", 
                            plateNumber, ticketNamesLog.toString().trim());
                        continue;
                    }
                    
                    // 7. 执行拉黑操作 - 拉黑月票记录中的所有车牌
                    String parkCode = getParkCodeByName(parkName);
                    if (parkCode == null) {
                        log.warn("⚠️ [120分钟拉黑] 无法获取车场编码 - 车场名称: {}", parkName);
                        continue;
                    }
                    
                    log.info("🚫 [120分钟拉黑] 开始拉黑 - 触发车牌: {}, 车场: {}, 停车时长: {}分钟", 
                        plateNumber, parkName, minutesAgo);
                    
                    // 收集该车牌所有月票记录中的所有车牌号
                    java.util.Set<String> allPlateNumbers = new java.util.HashSet<>();
                    for (MonthTick ticket : monthTickets) {
                        String carNo = ticket.getCarNo();
                        if (carNo != null && !carNo.trim().isEmpty()) {
                            String[] plates = carNo.split(",");
                            for (String plate : plates) {
                                String trimmedPlate = plate.trim();
                                if (!trimmedPlate.isEmpty()) {
                                    allPlateNumbers.add(trimmedPlate);
                                }
                            }
                        }
                    }
                    
                    log.info("📋 [120分钟拉黑] 该月票记录包含 {} 个车牌号: {}", 
                        allPlateNumbers.size(), allPlateNumbers);
                    
                    // 对所有车牌执行拉黑操作
                    int successCount = 0;
                    for (String plate : allPlateNumbers) {
                        try {
                            // 检查是否最近已拉黑（避免重复拉黑同一车牌）
                            if (isRecentlyBlacklisted(plate)) {
                                log.info("⏭️ [120分钟拉黑] 车牌 {} 最近已拉黑，跳过", plate);
                                continue;
                            }
                            
                            // 调用艾科平台拉黑接口（不添加违规记录，只拉黑）
                            boolean blacklistResult = callAikeBlacklistApi(plate, parkCode, "120分钟超时停车", monthTickets);
                            
                            if (blacklistResult) {
                                // 标记为已拉黑（避免重复拉黑）
                                markAsBlacklisted(plate);
                                successCount++;
                                log.info("✅ [120分钟拉黑] 车牌 {} 拉黑成功", plate);
                            } else {
                                log.error("❌ [120分钟拉黑] 车牌 {} 调用外部接口失败", plate);
                            }
                        } catch (Exception e) {
                            log.error("❌ [120分钟拉黑] 拉黑车牌 {} 失败", plate, e);
                        }
                    }
                    
                    blacklistedCount += successCount;
                    log.info("✅ [120分钟拉黑] 该月票批量拉黑完成 - 成功拉黑: {}辆", successCount);
                    
                    // 🆕 更新预约记录：标记为"已拉黑"状态，并记录拉黑的车牌列表
                    if (successCount > 0) {
                        try {
                            String blacklistedPlatesStr = String.join("、", allPlateNumbers);
                            appointment.setVenuestatus("已拉黑");
                            appointment.setRefusereason("拉黑车牌: " + blacklistedPlatesStr);
                            appointmentService.updateById(appointment);
                            log.info("✅ [120分钟拉黑] 已更新预约记录状态 - ID: {}, 状态: 已拉黑, 车牌: {}", 
                                appointment.getId(), blacklistedPlatesStr);
                        } catch (Exception e) {
                            log.error("❌ [120分钟拉黑] 更新预约记录状态失败 - ID: {}", appointment.getId(), e);
                        }
                    }
                    
                    // 发送拉黑通知（只通知触发车辆的车主、管家、巡检员，只发送一次）
                    if (successCount > 0) {
                        sendBlacklistNotifications(appointment, plateNumber, allPlateNumbers, successCount);
                    }
                    
                } catch (Exception e) {
                    log.error("❌ [120分钟拉黑] 处理车辆异常 - 车牌: {}", 
                        appointment.getPlatenumber(), e);
                }
            }
            
            log.info("✅ [120分钟拉黑] 完成 - 共检查: {}辆, 拉黑: {}辆", appointments.size(), blacklistedCount);
            
        } catch (Exception e) {
            log.error("❌ [120分钟拉黑] 执行异常", e);
        }
        
        return blacklistedCount;
    }
    
    /**
     * 获取允许拉黑的月票类型列表
     * @return 月票类型列表
     */
    private List<String> getAllowedTicketTypesForBlacklist() {
        try {
            // 从配置中获取（可以配置为所有车场或特定车场）
            String parkCode = "2KST9MNP";  // 万象上东车场编码，可以根据需要调整
            Map<String, Object> config = violationsService.getMonthlyTicketTimeoutConfig(parkCode);
            
            if (config == null) {
                log.info("⚠️ [拉黑配置] 未找到配置");
                return null;
            }
            
            // 尝试从 description JSON 中解析
            String description = (String) config.get("description");
            if (description != null && description.trim().startsWith("{")) {
                try {
                    JSONObject descJson = JSON.parseObject(description);
                    List<String> ticketTypes = descJson.getJSONArray("vipTicketTypes") != null 
                        ? descJson.getJSONArray("vipTicketTypes").toJavaList(String.class) 
                        : null;
                    if (ticketTypes != null && !ticketTypes.isEmpty()) {
                        return ticketTypes;
                    }
                } catch (Exception e) {
                    log.warn("⚠️ [拉黑配置] 解析 description JSON 失败: {}", e.getMessage());
                }
            }
            
            // 从顶层读取（向后兼容）
            @SuppressWarnings("unchecked")
            List<String> topLevelTypes = (List<String>) config.get("vipTicketTypes");
            return topLevelTypes;
            
        } catch (Exception e) {
            log.error("❌ [拉黑配置] 获取月票类型列表异常", e);
            return null;
        }
    }
    
    /**
     * 根据车场名称获取车场编码
     * @param parkName 车场名称
     * @return 车场编码
     */
    private String getParkCodeByName(String parkName) {
        // 简单映射，可以根据实际情况调整或从数据库查询
        if ("万象上东".equals(parkName)) {
            return "2KST9MNP";
        }
        // 可以添加更多车场映射
        // 或者从数据库查询
        return "2KST9MNP";  // 默认返回万象上东
    }
    
    /**
     * 发送拉黑通知给车主、巡检员、管家（只发送一次）
     * @param appointment 预约记录（触发拉黑的车辆）
     * @param triggerPlateNumber 触发拉黑的车牌号
     * @param blacklistedPlates 所有被拉黑的车牌集合
     * @param successCount 成功拉黑的车牌数量
     */
    private void sendBlacklistNotifications(Appointment appointment, String triggerPlateNumber, 
            java.util.Set<String> blacklistedPlates, int successCount) {
        try {
            log.info("📢 [拉黑通知] 开始发送 - 触发车牌: {}, 车场: {}, 拉黑数量: {}辆", 
                triggerPlateNumber, appointment.getCommunity(), successCount);
            
            // 计算停车时长（arrivedate是String类型，需要解析）
            String arrivedate = appointment.getArrivedate();
            if (!StringUtils.hasText(arrivedate)) {
                log.warn("⚠️ [拉黑通知] 进场时间为空，无法发送通知 - 触发车牌: {}", triggerPlateNumber);
                return;
            }
            
            // 解析进场时间字符串为LocalDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime enterTime;
            try {
                enterTime = LocalDateTime.parse(arrivedate, formatter);
            } catch (Exception e) {
                log.warn("⚠️ [拉黑通知] 进场时间格式错误 - 触发车牌: {}, 时间: {}", 
                    triggerPlateNumber, arrivedate);
                return;
            }
            
            LocalDateTime now = LocalDateTime.now();
            long parkingSeconds = Duration.between(enterTime, now).getSeconds();
            String parkingDuration = formatDuration(parkingSeconds);
            
            // 进场时间字符串（直接使用arrivedate）
            String enterTimeStr = arrivedate;
            
            // 获取拉黑天数（从配置中读取）
            Integer blacklistDays = 9999;  // 默认永久
            try {
                String parkCode = getParkCodeByName(appointment.getCommunity());
                Map<String, Object> config = violationsService.getMonthlyTicketTimeoutConfig(parkCode);
                if (config != null) {
                    String description = (String) config.get("description");
                    if (description != null && description.trim().startsWith("{")) {
                        JSONObject descJson = JSON.parseObject(description);
                        Boolean isPermanent = descJson.getBoolean("isPermanent");
                        if (isPermanent != null && !isPermanent) {
                            blacklistDays = descJson.getInteger("blacklistDays");
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ [拉黑通知] 获取拉黑天数配置失败，使用默认值: {}", e.getMessage());
            }
            
            int sentCount = 0;  // 已发送通知数量
            int totalCount = 0;  // 总发送目标数量
            
            // 拼接所有被拉黑的车牌信息（用于通知内容）
            String blacklistedPlatesInfo = String.join("、", blacklistedPlates);
            log.info("📋 [拉黑通知] 被拉黑的车牌: {}", blacklistedPlatesInfo);
            
            // 1️⃣ 发送给车主（从预约表获取openid）
            if (StringUtils.hasText(appointment.getOpenid())) {
                totalCount++;
                log.info("📧 [拉黑通知] 发送给车主 - 触发车牌: {}, openid: {}", 
                    triggerPlateNumber, appointment.getOpenid());
                
                Map<String, Object> result = weChatTemplateMessageService.sendBlacklistAddNotification(
                    appointment.getOpenid(), 
                    triggerPlateNumber,  // 使用触发车牌作为主要显示
                    appointment.getCommunity(), 
                    enterTimeStr, 
                    parkingDuration, 
                    blacklistDays
                );
                
                Boolean success = (Boolean) result.get("success");
                if (Boolean.TRUE.equals(success)) {
                    sentCount++;
                    log.info("✅ [拉黑通知] 车主通知发送成功");
                } else {
                    log.warn("⚠️ [拉黑通知] 车主通知发送失败: {}", result.get("message"));
                }
            } else {
                log.debug("⏭️ [拉黑通知] 无车主openid，跳过车主通知");
            }
            
            // 2️⃣ 发送给管家（从预约表获取auditopenid）
            if (StringUtils.hasText(appointment.getAuditopenid())) {
                totalCount++;
                log.info("📧 [拉黑通知] 发送给管家 - openid: {}", appointment.getAuditopenid());
                
                Map<String, Object> result = weChatTemplateMessageService.sendBlacklistAddNotification(
                    appointment.getAuditopenid(), 
                    triggerPlateNumber,  // 使用触发车牌作为主要显示
                    appointment.getCommunity(), 
                    enterTimeStr, 
                    parkingDuration, 
                    blacklistDays
                );
                
                Boolean success = (Boolean) result.get("success");
                if (Boolean.TRUE.equals(success)) {
                    sentCount++;
                    log.info("✅ [拉黑通知] 管家通知发送成功");
                } else {
                    log.warn("⚠️ [拉黑通知] 管家通知发送失败: {}", result.get("message"));
                }
            } else {
                log.debug("⏭️ [拉黑通知] 无管家openid，跳过管家通知");
            }
            
            // 3️⃣ 发送给值班巡检员
            if (StringUtils.hasText(appointment.getCommunity())) {
                List<Patrol> onDutyPatrols = patrolService.getOnDutyPatrolsByCommunity(appointment.getCommunity());
                
                if (!onDutyPatrols.isEmpty()) {
                    log.info("📋 [拉黑通知] 向 {} 位值班巡检员发送通知 - 车场: {}", 
                        onDutyPatrols.size(), appointment.getCommunity());
                    
                    for (Patrol patrol : onDutyPatrols) {
                        if (StringUtils.hasText(patrol.getOpenid())) {
                            totalCount++;
                            log.info("📧 [拉黑通知] 发送给巡检员 - 姓名: {}, openid: {}", 
                                patrol.getUsername(), patrol.getOpenid());
                            
                            Map<String, Object> result = weChatTemplateMessageService.sendBlacklistAddNotification(
                                patrol.getOpenid(), 
                                triggerPlateNumber,  // 使用触发车牌作为主要显示
                                appointment.getCommunity(), 
                                enterTimeStr, 
                                parkingDuration, 
                                blacklistDays
                            );
                            
                            Boolean success = (Boolean) result.get("success");
                            if (Boolean.TRUE.equals(success)) {
                                sentCount++;
                                log.info("✅ [拉黑通知] 巡检员({})通知发送成功", patrol.getUsername());
                            } else {
                                log.warn("⚠️ [拉黑通知] 巡检员({})通知发送失败: {}", 
                                    patrol.getUsername(), result.get("message"));
                            }
                        }
                    }
                } else {
                    log.warn("⚠️ [拉黑通知] 车场 {} 当前无值班巡检员", appointment.getCommunity());
                }
            }
            
            log.info("📊 [拉黑通知] 发送完成 - 触发车牌: {}, 拉黑{}辆, 通知成功: {}/{}", 
                triggerPlateNumber, successCount, sentCount, totalCount);
            
        } catch (Exception e) {
            log.error("❌ [拉黑通知] 发送异常 - 触发车牌: {}", triggerPlateNumber, e);
        }
    }
    
    /**
     * 格式化时长
     * @param seconds 秒数
     * @return 格式化后的时长字符串（HH:MM:SS格式）
     */
    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    
    /**
     * 调用艾科平台拉黑接口
     * @param plateNumber 车牌号
     * @param parkCode 车场编码
     * @param reason 拉黑原因
     * @param monthTickets 月票列表
     * @return 是否拉黑成功
     */
    private boolean callAikeBlacklistApi(String plateNumber, String parkCode, String reason, List<MonthTick> monthTickets) {
        try {
            log.info("📡 [艾科拉黑] 调用外部接口 - 车牌: {}, 车场: {}, 原因: {}", 
                plateNumber, parkCode, reason);
            
            // 1. 从月票列表中获取车主姓名
            String carOwner = "系统";  // 默认值
            if (monthTickets != null && !monthTickets.isEmpty()) {
                for (MonthTick ticket : monthTickets) {
                    if (ticket.getUserName() != null && !ticket.getUserName().trim().isEmpty()) {
                        carOwner = ticket.getUserName();
                        break;  // 使用第一个有效的用户名
                    }
                }
            }
            
            // 2. 从配置中获取拉黑参数
            String specialCarTypeId = "";  // 黑名单ID
            Integer blacklistDays = 9999;  // 默认永久
            boolean isPermanent = true;  // 默认永久拉黑
            
            Map<String, Object> config = violationsService.getMonthlyTicketTimeoutConfig(parkCode);
            if (config != null) {
                String description = (String) config.get("description");
                if (description != null && description.trim().startsWith("{")) {
                    try {
                        JSONObject descJson = JSON.parseObject(description);
                        
                        // blacklistName格式: "481|万象违停过夜超时车辆、跟车"
                        // specialCarTypeId就是前面的数字: "481"
                        String blacklistName = descJson.getString("blacklistName");
                        if (blacklistName != null && blacklistName.contains("|")) {
                            specialCarTypeId = blacklistName.split("\\|")[0];  // 取"|"前面的ID
                        } else {
                            specialCarTypeId = blacklistName;  // 如果没有"|"，直接使用
                        }
                        
                        // 是否永久拉黑
                        Boolean isPermanentConfig = descJson.getBoolean("isPermanent");
                        if (isPermanentConfig != null) {
                            isPermanent = isPermanentConfig;
                            if (!isPermanent) {
                                // 临时拉黑，获取天数
                                Integer days = descJson.getInteger("blacklistDays");
                                if (days != null) {
                                    blacklistDays = days;
                                }
                            }
                        }
                        
                        log.info("📋 [艾科拉黑] 配置解析 - specialCarTypeId: {}, isPermanent: {}, days: {}",
                            specialCarTypeId, isPermanent, blacklistDays);
                        
                    } catch (Exception e) {
                        log.warn("⚠️ [艾科拉黑] 解析配置失败，使用默认值: {}", e.getMessage());
                    }
                }
            }
            
            // 如果没有配置，使用默认值
            if (specialCarTypeId == null || specialCarTypeId.isEmpty()) {
                specialCarTypeId = "481";  // 默认黑名单ID
            }
            
            // 3. 构造接口参数
            java.util.HashMap<String, Object> params = new java.util.HashMap<>();
            params.put("parkCode", parkCode);
            params.put("carCode", plateNumber);
            params.put("carOwner", carOwner);  // 从月票表获取的车主姓名
            params.put("reason", reason);
            params.put("isPermament", isPermanent ? "1" : "0");  // 1=永久，0=临时
            params.put("specialCarTypeId", specialCarTypeId);  // 黑名单ID（不含"|"后的名称）
            params.put("timePeriod", isPermanent ? "" : String.valueOf(blacklistDays));  // 永久时为空
            params.put("remark1", "定时任务自动拉黑");
            params.put("remark2", reason);
            
            log.info("📤 [艾科拉黑] 请求参数 - carOwner: {}, isPermament: {}, specialCarTypeId: {}, timePeriod: {}",
                carOwner, isPermanent ? "1" : "0", specialCarTypeId, isPermanent ? "" : blacklistDays);
            
            // 调用艾科平台接口
            JSONObject result = aikeConfig.downHandler(
                AIKEConfig.AK_URL, 
                AIKEConfig.AK_KEY, 
                AIKEConfig.AK_SECRET, 
                "addBlackListCar", 
                params
            );
            
            // 打印完整响应内容
            log.info("📄 [艾科拉黑] 接口完整响应 - 车牌: {}, 响应: {}", plateNumber, result);
            
            if (result != null) {
                // 艾科接口响应格式：{resultCode: 0, status: 1, message: "xxx", data: {}}
                Integer resultCode = result.getInteger("resultCode");
                Integer status = result.getInteger("status");
                String message = result.getString("message");
                
                // 打印所有字段
                log.info("🔍 [艾科拉黑] 响应字段 - resultCode: {}, status: {}, message: {}", 
                    resultCode, status, message);
                
                // status=1 表示成功
                if (status != null && status == 1) {
                    log.info("✅ [艾科拉黑] 接口调用成功 - 车牌: {}, 响应: {}", plateNumber, message);
                    return true;
                } else {
                    log.error("❌ [艾科拉黑] 接口调用失败 - 车牌: {}, resultCode: {}, status: {}, message: {}", 
                        plateNumber, resultCode, status, message);
                    return false;
                }
            } else {
                log.error("❌ [艾科拉黑] 接口返回空 - 车牌: {}", plateNumber);
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ [艾科拉黑] 调用异常 - 车牌: {}", plateNumber, e);
            return false;
        }
    }
    
    // ==================== 缓存管理方法 ====================
    
    /**
     * 生成缓存键
     * @param appointmentId 预约ID
     * @param notifyPoint 通知时间点（15/5/1分钟）
     * @return 缓存键
     */
    private String generateCacheKey(Integer appointmentId, long notifyPoint) {
        return appointmentId + "_" + notifyPoint;
    }
    
    /**
     * 检查是否已发送过
     * @param cacheKey 缓存键
     * @return true=已发送，false=未发送
     */
    private boolean isAlreadySent(String cacheKey) {
        LocalDateTime lastSentTime = notificationSentCache.get(cacheKey);
        if (lastSentTime == null) {
            return false;
        }
        
        // 如果距离上次发送不到5分钟，认为是重复发送
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        return lastSentTime.isAfter(fiveMinutesAgo);
    }
    
    /**
     * 标记为已发送
     * @param cacheKey 缓存键
     */
    private void markAsSent(String cacheKey) {
        notificationSentCache.put(cacheKey, LocalDateTime.now());
        log.debug("📝 [缓存] 记录发送 - Key: {}, 时间: {}", cacheKey, LocalDateTime.now());
    }
    
    /**
     * 清理过期的缓存记录（超过30分钟的）
     */
    private void cleanExpiredCache() {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);
        int beforeSize = notificationSentCache.size();
        
        notificationSentCache.entrySet().removeIf(entry -> 
            entry.getValue().isBefore(thirtyMinutesAgo)
        );
        
        int afterSize = notificationSentCache.size();
        if (beforeSize > afterSize) {
            log.debug("🧹 [缓存清理] 清理过期记录 - 清理前: {}, 清理后: {}, 删除: {}", 
                beforeSize, afterSize, beforeSize - afterSize);
        }
    }
    
    // ==================== 万象上东拉黑检查 ====================
    
    /**
     * 检查万象上东VIP月票车拉黑条件
     * 
     * 检查逻辑：
     * 1. 获取万象上东的配置
     * 2. 查询该车场有效的月票车辆
     * 3. 检查是否满足拉黑条件：
     *    - 夜间时间段进场（如 23:00-06:00）
     *    - VIP月票类型在待检查列表中
     *    - 停车时长超过阈值（如 2小时）
     * 4. 调用批量拉黑方法
     */
    private void checkWanXiangVipBlacklist() {
        try {
            log.debug("🌙 [万象上东检查] 开始检查VIP月票车拉黑条件");
            
            String parkCode = "2KST9MNP";  // 万象上东车场编码
            String parkName = "万象上东";
            
            // 1. 获取配置
            Map<String, Object> config = violationsService.getMonthlyTicketTimeoutConfig(parkCode);
            if (config == null) {
                log.debug("⏭️ [万象上东检查] 未配置规则，跳过检查");
                return;
            }
            
            // 检查是否启用过夜检查（数据库中为Integer类型：1=启用，0=禁用）
            Object enableOvernightCheckObj = config.get("enableOvernightCheck");
            boolean enableOvernightCheck = false;
            if (enableOvernightCheckObj instanceof Integer) {
                enableOvernightCheck = ((Integer) enableOvernightCheckObj) == 1;
            } else if (enableOvernightCheckObj instanceof Boolean) {
                enableOvernightCheck = (Boolean) enableOvernightCheckObj;
            }
            
            if (!enableOvernightCheck) {
                log.debug("⏭️ [万象上东检查] 过夜检查未启用，跳过");
                return;
            }
            
            // 获取配置参数（优先从 description JSON 中读取）
            String nightStartTime = (String) config.get("nightStartTime");  // 如 "23:00"
            String nightEndTime = (String) config.get("nightEndTime");      // 如 "06:00"
            Integer nightTimeHours = (Integer) config.get("nightTimeHours"); // 如 2
            String vipCheckMode = null;
            List<String> vipTicketTypes = null;
            
            // 📝 尝试从 description JSON 中解析 VIP 配置
            String description = (String) config.get("description");
            if (description != null && description.trim().startsWith("{")) {
                try {
                    JSONObject descJson = JSON.parseObject(description);
                    vipCheckMode = descJson.getString("vipCheckMode");
                    vipTicketTypes = descJson.getJSONArray("vipTicketTypes") != null 
                        ? descJson.getJSONArray("vipTicketTypes").toJavaList(String.class) 
                        : null;
                    log.debug("✅ [VIP配置] 从 description 解析: 模式={}, 类型数量={}", 
                        vipCheckMode, vipTicketTypes != null ? vipTicketTypes.size() : 0);
                } catch (Exception e) {
                    log.warn("⚠️ [VIP配置] 解析 description JSON 失败: {}", e.getMessage());
                }
            }
            
            // 如果 description 中没有，尝试从顶层读取（向后兼容）
            if (vipCheckMode == null) {
                vipCheckMode = (String) config.get("vipCheckMode");
            }
            if (vipTicketTypes == null) {
                @SuppressWarnings("unchecked")
                List<String> topLevelTypes = (List<String>) config.get("vipTicketTypes");
                vipTicketTypes = topLevelTypes;
            }
            
            if (nightStartTime == null || nightEndTime == null || nightTimeHours == null) {
                log.warn("⚠️ [万象上东检查] 配置不完整，跳过检查");
                return;
            }
            
            log.info("📋 [万象上东检查] 配置: 夜间{}~{}, 超过{}小时, 模式:{}, VIP类型:{}", 
                nightStartTime, nightEndTime, nightTimeHours, vipCheckMode, vipTicketTypes);
            
            // 2. 查询该车场所有有效的VIP月票车辆
            // 注意：这里查询所有有效月票车，然后筛选VIP类型
            List<MonthTick> allMonthTickets = monthTicketService.lambdaQuery()
                .eq(MonthTick::getParkName, parkName)
                .eq(MonthTick::getValidStatus, 1)  // 有效状态
                .eq(MonthTick::getIsFrozen, 0)     // 未冻结
                .isNotNull(MonthTick::getUserPhone) // 必须有手机号
                .list();
            
            if (allMonthTickets.isEmpty()) {
                log.debug("✅ [万象上东检查] 无有效月票车辆");
                return;
            }
            
            log.info("📊 [万象上东检查] 查询到 {} 辆有效月票车", allMonthTickets.size());
            
            // 3. 筛选符合VIP类型检查条件的车辆
            int checkedCount = 0;
            int blacklistedCount = 0;
            
            for (MonthTick ticket : allMonthTickets) {
                try {
                    String carNo = ticket.getCarNo();
                    String ticketName = ticket.getTicketName();
                    
                    // 检查是否应该检查该VIP类型
                    boolean shouldCheck = shouldCheckVipType(ticketName, vipCheckMode, vipTicketTypes);
                    if (!shouldCheck) {
                        continue;
                    }
                    
                    checkedCount++;
                    
                    // 检查缓存，避免短时间内重复拉黑同一车辆
                    if (isRecentlyBlacklisted(carNo)) {
                        log.debug("⏭️ [万象上东检查] 车牌 {} 最近已拉黑，跳过", carNo);
                        continue;
                    }
                    
                    // 4. 获取车辆进场时间
                    LocalDateTime enterTime = getVehicleEnterTime(carNo);
                    if (enterTime == null) {
                        log.debug("⏭️ [万象上东检查] 车牌 {} 未找到进场记录，跳过", carNo);
                        continue;
                    }
                    
                    log.debug("🔍 [万象上东检查] 车牌: {}, 月票类型: {}, 业主手机: {}, 进场时间: {}", 
                        carNo, ticketName, ticket.getUserPhone(), enterTime);
                    
                    // 5. 判断是否在夜间时间段进场
                    boolean isNight = isNightEntry(enterTime, nightStartTime, nightEndTime);
                    if (!isNight) {
                        log.debug("⏭️ [万象上东检查] 车牌 {} 非夜间进场（{}），跳过", carNo, enterTime.toLocalTime());
                        continue;
                    }
                    
                    // 6. 判断停车时长是否超过阈值
                    boolean isExceeded = isParkingTimeExceeded(enterTime, nightTimeHours);
                    if (!isExceeded) {
                        long parkingHours = Duration.between(enterTime, LocalDateTime.now()).toHours();
                        log.debug("⏭️ [万象上东检查] 车牌 {} 停车时长 {}小时，未超过 {}小时阈值，跳过", 
                            carNo, parkingHours, nightTimeHours);
                        continue;
                    }
                    
                    // 7. 满足所有条件，执行批量拉黑
                    long parkingHours = Duration.between(enterTime, LocalDateTime.now()).toHours();
                    log.info("🚫 [万象上东检查] 车牌 {} 满足拉黑条件：夜间进场（{}），停车 {}小时，开始批量拉黑", 
                        carNo, enterTime.toLocalTime(), parkingHours);
                    
                    vehicleReservationController.processWanXiangBlacklistByOwner(carNo, parkCode);
                    markAsBlacklisted(carNo);
                    blacklistedCount++;
                    
                } catch (Exception e) {
                    log.warn("⚠️ [万象上东检查] 处理车辆异常: {}, 错误: {}", 
                        ticket.getCarNo(), e.getMessage());
                }
            }
            
            log.info("✅ [万象上东检查] 完成 - 总计: {}辆, 检查: {}辆, 拉黑: {}辆", 
                allMonthTickets.size(), checkedCount, blacklistedCount);
            
        } catch (Exception e) {
            log.error("❌ [万象上东检查] 执行异常", e);
        }
    }
    
    /**
     * 判断是否应该检查该VIP类型
     * 
     * @param ticketName VIP月票类型名称
     * @param vipCheckMode 检查模式：include=待检查，exclude=免检
     * @param vipTicketTypes VIP类型列表
     * @return true=应该检查，false=不检查
     */
    private boolean shouldCheckVipType(String ticketName, String vipCheckMode, List<String> vipTicketTypes) {
        if (ticketName == null || vipTicketTypes == null || vipTicketTypes.isEmpty()) {
            return false;
        }
        
        boolean isInList = vipTicketTypes.contains(ticketName.trim());
        
        if ("include".equals(vipCheckMode)) {
            // 待检查模式：只检查列表中的类型
            return isInList;
        } else {
            // 免检模式：检查所有类型，除了列表中的
            return !isInList;
        }
    }
    
    /**
     * 检查车辆是否最近被拉黑过（避免重复拉黑）
     * 
     * @param carNo 车牌号
     * @return true=最近拉黑过，false=未拉黑或已过期
     */
    private boolean isRecentlyBlacklisted(String carNo) {
        LocalDateTime lastBlacklistTime = wanXiangBlacklistCache.get(carNo);
        if (lastBlacklistTime == null) {
            return false;
        }
        
        // 如果距离上次拉黑不到1小时，认为是最近拉黑过
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        return lastBlacklistTime.isAfter(oneHourAgo);
    }
    
    /**
     * 标记车辆为已拉黑
     * 
     * @param carNo 车牌号
     */
    private void markAsBlacklisted(String carNo) {
        wanXiangBlacklistCache.put(carNo, LocalDateTime.now());
        log.debug("📝 [万象上东缓存] 标记已拉黑 - 车牌: {}, 时间: {}", carNo, LocalDateTime.now());
    }
    
    /**
     * 获取车辆的进场时间
     * 
     * @param carNo 车牌号
     * @return 进场时间，如果未找到返回 null
     */
    private LocalDateTime getVehicleEnterTime(String carNo) {
        try {
            // 查询最近的有效预约记录，使用 arrivedate（到达时间）字段
            Appointment appointment = appointmentService.lambdaQuery()
                .eq(Appointment::getPlatenumber, carNo)
                .eq(Appointment::getVenuestatus, "在场")  // 只查询在场状态的预约
                .isNotNull(Appointment::getArrivedate)  // 必须有到达时间
                .orderByDesc(Appointment::getRecorddate)  // 按记录时间倒序
                .last("LIMIT 1")  // 只取最新一条
                .one();
            
            if (appointment != null && appointment.getArrivedate() != null) {
                try {
                    // arrivedate 是字符串类型，需要解析为 LocalDateTime
                    // 格式可能是：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd'T'HH:mm:ss
                    String arrivedate = appointment.getArrivedate();
                    if (arrivedate.contains("T")) {
                        return LocalDateTime.parse(arrivedate.replace("T", " "), 
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    } else {
                        return LocalDateTime.parse(arrivedate, 
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }
                } catch (Exception parseEx) {
                    log.warn("⚠️ [进场时间] 解析到达时间失败: carNo={}, arrivedate={}", carNo, appointment.getArrivedate());
                    return null;
                }
            }
            
            return null;
        } catch (Exception e) {
            log.warn("⚠️ [进场时间] 查询车牌 {} 的进场时间异常: {}", carNo, e.getMessage());
            return null;
        }
    }
    
    /**
     * 判断是否在夜间时间段进场
     * 
     * @param enterTime 进场时间
     * @param nightStartTime 夜间开始时间（如 "23:00"）
     * @param nightEndTime 夜间结束时间（如 "06:00"）
     * @return true=夜间进场，false=非夜间进场
     */
    private boolean isNightEntry(LocalDateTime enterTime, String nightStartTime, String nightEndTime) {
        if (enterTime == null || nightStartTime == null || nightEndTime == null) {
            return false;
        }
        
        try {
            LocalTime enterTimeOnly = enterTime.toLocalTime();
            LocalTime nightStart = LocalTime.parse(nightStartTime);  // 如 23:00
            LocalTime nightEnd = LocalTime.parse(nightEndTime);      // 如 06:00
            
            // 跨日判断（如 23:00-06:00）
            // 23:00 之后或 06:00 之前都算夜间
            if (nightStart.isAfter(nightEnd)) {
                boolean isAfterStart = !enterTimeOnly.isBefore(nightStart);  // >= 23:00
                boolean isBeforeEnd = enterTimeOnly.isBefore(nightEnd);      // < 06:00
                return isAfterStart || isBeforeEnd;
            } else {
                // 同日判断（如 01:00-05:00）
                // 01:00 之后且 05:00 之前
                return !enterTimeOnly.isBefore(nightStart) && enterTimeOnly.isBefore(nightEnd);
            }
            
        } catch (Exception e) {
            log.warn("⚠️ [夜间判断] 解析时间异常: nightStart={}, nightEnd={}, error={}", 
                nightStartTime, nightEndTime, e.getMessage());
            return false;
        }
    }
    
    /**
     * 判断停车时长是否超过阈值
     * 
     * @param enterTime 进场时间
     * @param thresholdHours 阈值（小时）
     * @return true=超过阈值，false=未超过
     */
    private boolean isParkingTimeExceeded(LocalDateTime enterTime, int thresholdHours) {
        if (enterTime == null) {
            return false;
        }
        
        try {
            LocalDateTime now = LocalDateTime.now();
            long parkingHours = Duration.between(enterTime, now).toHours();
            
            return parkingHours >= thresholdHours;
            
        } catch (Exception e) {
            log.warn("⚠️ [停车时长] 计算异常: enterTime={}, error={}", enterTime, e.getMessage());
            return false;
        }
    }
    
    // ==================== 推送时间段控制 ====================
    
    /**
     * 检查当前时间是否在推送时间段内
     * 
     * 推送时间段从配置表的 description 字段中读取（JSON格式）：
     * {
     *   "notificationStartTime": "23:00",
     *   "notificationEndTime": "06:00"
     * }
     * 
     * 如果未配置，则使用默认值：23:00-06:00
     * 
     * @return true=在推送时间段内，false=不在推送时间段内
     */
    private boolean isInNotificationTimeRange() {
        try {
            LocalTime now = LocalTime.now();
            
            // 1. 尝试从配置表读取推送时间段
            String notificationStartTime = DEFAULT_NOTIFICATION_START_TIME;
            String notificationEndTime = DEFAULT_NOTIFICATION_END_TIME;
            
            try {
                // 查询任意一条有效配置（优先查询启用的）
                MonthlyTicketTimeoutConfig config = monthlyTicketTimeoutConfigService.lambdaQuery()
                    .eq(MonthlyTicketTimeoutConfig::getIsActive, true)
                    .isNotNull(MonthlyTicketTimeoutConfig::getDescription)
                    .last("LIMIT 1")
                    .one();
                
                if (config != null && StringUtils.hasText(config.getDescription())) {
                    // 尝试解析 description 中的 JSON 配置
                    try {
                        JSONObject descJson = JSON.parseObject(config.getDescription());
                        String configStartTime = descJson.getString("notificationStartTime");
                        String configEndTime = descJson.getString("notificationEndTime");
                        
                        if (StringUtils.hasText(configStartTime) && StringUtils.hasText(configEndTime)) {
                            notificationStartTime = configStartTime;
                            notificationEndTime = configEndTime;
                            log.debug("📋 [推送时间段] 从配置表读取: {}~{}", notificationStartTime, notificationEndTime);
                        } else {
                            log.debug("⚠️ [推送时间段] 配置不完整，使用默认值: {}~{}", 
                                DEFAULT_NOTIFICATION_START_TIME, DEFAULT_NOTIFICATION_END_TIME);
                        }
                    } catch (Exception e) {
                        log.debug("📝 [推送时间段] description为旧格式文本，使用默认值: {}~{} (提示：重新保存配置可升级为新格式)", 
                            DEFAULT_NOTIFICATION_START_TIME, DEFAULT_NOTIFICATION_END_TIME);
                    }
                } else {
                    log.debug("📋 [推送时间段] 未找到配置，使用默认值: {}~{}", 
                        DEFAULT_NOTIFICATION_START_TIME, DEFAULT_NOTIFICATION_END_TIME);
                }
            } catch (Exception e) {
                log.warn("⚠️ [推送时间段] 查询配置异常，使用默认值: {}~{}", 
                    DEFAULT_NOTIFICATION_START_TIME, DEFAULT_NOTIFICATION_END_TIME, e);
            }
            
            // 2. 解析时间
            LocalTime startTime = LocalTime.parse(notificationStartTime);
            LocalTime endTime = LocalTime.parse(notificationEndTime);
            
            // 3. 判断当前时间是否在推送时间段内
            boolean isInRange;
            if (startTime.isAfter(endTime)) {
                // 跨日情况（如 23:00-06:00）
                // 23:00 之后或 06:00 之前都算在时间段内
                isInRange = !now.isBefore(startTime) || now.isBefore(endTime);
            } else {
                // 同日情况（如 08:00-18:00）
                // 08:00 之后且 18:00 之前
                isInRange = !now.isBefore(startTime) && now.isBefore(endTime);
            }
            
            if (isInRange) {
                log.debug("✅ [推送时间段] 当前时间 {} 在推送时间段 {}~{} 内", now, notificationStartTime, notificationEndTime);
            } else {
                log.debug("⏭️ [推送时间段] 当前时间 {} 不在推送时间段 {}~{} 内", now, notificationStartTime, notificationEndTime);
            }
            
            return isInRange;
            
        } catch (Exception e) {
            log.error("❌ [推送时间段] 判断异常，默认允许推送", e);
            return true; // 异常情况下默认允许推送，避免影响功能
        }
    }
    
    /**
     * 🆕 智能推送时间判断：根据进场时间+停车时长计算当前时间是否在夜间时段
     * 
     * 逻辑：进场时间 + 停车时长 = 当前时间，判断当前时间是否在夜间时段（23:00-06:00）
     * 例如：早上9点进场，停车15小时 → 当前时间=9+15=24点 → 在夜间时段 → 推送
     * 
     * @param arriveDateTime 进场时间
     * @param parkingMinutes 停车时长（分钟）
     * @param parkCode 车场编码（用于查询对应车场的配置）
     * @return true=在夜间时段，应推送；false=不在夜间时段，不推送
     */
    private boolean isInNotificationTimeByDuration(LocalDateTime arriveDateTime, long parkingMinutes, String parkCode) {
        try {
            // 1. 计算当前时间 = 进场时间 + 停车时长
            LocalDateTime currentTime = arriveDateTime.plusMinutes(parkingMinutes);
            LocalTime currentTimeOnly = currentTime.toLocalTime();
            
            // 2. 从配置表读取夜间时段配置
            String nightStartTime = DEFAULT_NOTIFICATION_START_TIME;  // 默认 23:00
            String nightEndTime = DEFAULT_NOTIFICATION_END_TIME;      // 默认 06:00
            
            try {
                MonthlyTicketTimeoutConfig config = monthlyTicketTimeoutConfigService.lambdaQuery()
                    .eq(MonthlyTicketTimeoutConfig::getParkCode, parkCode)
                    .eq(MonthlyTicketTimeoutConfig::getIsActive, true)
                    .isNotNull(MonthlyTicketTimeoutConfig::getDescription)
                    .last("LIMIT 1")
                    .one();
                
                log.debug("📋 [智能时间判断] 查询到的配置 - parkCode: {}, config: {}", parkCode, config);
                if (config != null && StringUtils.hasText(config.getDescription())) {
                    try {
                        JSONObject descJson = JSON.parseObject(config.getDescription());
                        String configStartTime = descJson.getString("notificationStartTime");
                        String configEndTime = descJson.getString("notificationEndTime");
                        
                        log.debug("📋 [智能时间判断] 解析配置 - 开始: {}, 结束: {}", configStartTime, configEndTime);
                        if (StringUtils.hasText(configStartTime) && StringUtils.hasText(configEndTime)) {
                            nightStartTime = configStartTime;
                            nightEndTime = configEndTime;
                        }
                    } catch (Exception ignored) {
                        // 使用默认值
                    }
                }
            } catch (Exception e) {
                log.debug("⚠️ [智能时间判断] 读取配置异常，使用默认值", e);
            }
            
            // 3. 解析时间并判断
            LocalTime startTime = LocalTime.parse(nightStartTime);
            LocalTime endTime = LocalTime.parse(nightEndTime);
            
            boolean isInNightTime;
            if (startTime.isAfter(endTime)) {
                // 跨日情况（如 23:00-06:00）
                isInNightTime = !currentTimeOnly.isBefore(startTime) || currentTimeOnly.isBefore(endTime);
            } else {
                // 同日情况（如 08:00-18:00）
                isInNightTime = !currentTimeOnly.isBefore(startTime) && currentTimeOnly.isBefore(endTime);
            }
            
            log.debug("🌙 [智能时间判断] 进场: {}, 停车: {}分钟, 当前: {} → 夜间时段: {}", 
                arriveDateTime.toLocalTime(), parkingMinutes, currentTimeOnly, isInNightTime);
            
            return isInNightTime;
            
        } catch (Exception e) {
            log.warn("⚠️ [智能时间判断] 异常，默认允许推送", e);
            return true;
        }
    }
    
    /**
     * 🔥 【修正版】发送滞留通知给车主
     * 
     * 30分钟、60分钟的滞留通知统一发给车主本人
     * - 邀请预约：发给访客（openid）
     * - 代人预约：发给业主（owneropenid）
     * - 其他类型：发给访客（openid）
     * 
     * @param appointment 预约信息
     * @param parkingMinutes 停车时长（分钟）
     * @return 是否发送成功
     */
    private boolean sendRetentionNotification(Appointment appointment, long parkingMinutes) {
        try {
            String appointType = appointment.getAppointtype();
            long parkingHours = parkingMinutes / 60;
            long remainingMinutes = parkingMinutes % 60;
            
            // 格式化滞留时间：30分钟→"30分钟"，60分钟→"1小时"
            String retentionTime;
            if (parkingHours > 0 && remainingMinutes > 0) {
                retentionTime = parkingHours + "小时" + remainingMinutes + "分钟";
            } else if (parkingHours > 0) {
                retentionTime = parkingHours + "小时";
            } else {
                retentionTime = remainingMinutes + "分钟";
            }
            
            log.info("🚗 [滞留通知-车主] 车牌: {}, 预约类型: {}, 滞留: {}", 
                appointment.getPlatenumber(), appointType, retentionTime);
            
            // 🔍 详细调试信息
            log.info("🔍 [调试] 预约ID: {}, 访客openid: {}, 业主openid: {}", 
                appointment.getId(), 
                appointment.getOpenid() == null ? "null" : (appointment.getOpenid().isEmpty() ? "empty" : "有值"),
                appointment.getOwneropenid() == null ? "null" : (appointment.getOwneropenid().isEmpty() ? "empty" : "有值"));
            
            int successCount = 0;
            int totalCount = 0;
            
            // 🔥 【修正】根据预约类型发送给对应的车主
            if ("邀请".equals(appointType)) {
                // 邀请预约：车主是访客本人
                if (StringUtils.hasText(appointment.getOpenid())) {
                    totalCount++;
                    log.info("📧 [滞留通知-车主] 邀请预约，发送给访客（车主） - openid: {}", appointment.getOpenid());
                    if (sendRetentionNotificationToUser(appointment.getOpenid(), appointment, retentionTime, "(访客-车主)")) {
                        successCount++;
                    }
                } else {
                    log.warn("⚠️ [滞留通知-车主] 访客openid为空，无法发送 - 车牌: {}, 预约类型: 邀请", 
                        appointment.getPlatenumber());
                }
            } else if ("代人".equals(appointType)) {
                // 代人预约：车主是业主
                if (StringUtils.hasText(appointment.getOwneropenid())) {
                    totalCount++;
                    log.info("📧 [滞留通知-车主] 代人预约，发送给业主（车主） - openid: {}", appointment.getOwneropenid());
                    if (sendRetentionNotificationToUser(appointment.getOwneropenid(), appointment, retentionTime, "(业主-车主)")) {
                        successCount++;
                    }
                } else {
                    log.warn("⚠️ [滞留通知-车主] 业主openid为空，无法发送 - 车牌: {}, 预约类型: 代人", 
                        appointment.getPlatenumber());
                }
            } else {
                // 其他类型：车主是访客本人
                if (StringUtils.hasText(appointment.getOpenid())) {
                    totalCount++;
                    log.info("📧 [滞留通知-车主] 其他类型，发送给访客（车主） - openid: {}", appointment.getOpenid());
                    if (sendRetentionNotificationToUser(appointment.getOpenid(), appointment, retentionTime, "(访客-车主)")) {
                        successCount++;
                    }
                } else {
                    log.warn("⚠️ [滞留通知-车主] 访客openid为空，无法发送 - 车牌: {}, 预约类型: {}", 
                        appointment.getPlatenumber(), appointType);
                }
            }
            
            log.info("📊 [滞留通知-车主] 发送完成 - 车牌: {}, 成功: {}/{}", 
                appointment.getPlatenumber(), successCount, totalCount);
            
            if (totalCount == 0) {
                log.warn("⚠️ [滞留通知-车主] 没有可发送的目标用户（车主openid为空） - 车牌: {}", appointment.getPlatenumber());
            }
            
            return successCount > 0;
            
        } catch (Exception e) {
            log.error("❌ [滞留通知-车主] 异常 - 车牌: {}", appointment.getPlatenumber(), e);
            return false;
        }
    }
    
    /**
     * 🆕 发送滞留通知给指定用户
     * 
     * @param openid 用户openid
     * @param appointment 预约信息
     * @param retentionTime 滞留时间
     * @param userType 用户类型描述
     * @return 是否发送成功
     */
    private boolean sendRetentionNotificationToUser(String openid, Appointment appointment, String retentionTime, String userType) {
        try {
            log.info("📨 [发送微信模板消息] 开始 - 用户类型: {}, openid: {}, 车牌: {}, 滞留时长: {}", 
                userType, openid, appointment.getPlatenumber(), retentionTime);
            
            java.util.Map<String, Object> sendResult = weChatTemplateMessageService.sendParkingRetentionNotification(
                openid,
                appointment.getPlatenumber(),
                appointment.getCommunity(),
                appointment.getArrivedate(),
                retentionTime
            );
            
            log.info("📬 [微信消息返回] 用户类型: {}, 返回结果: {}", userType, sendResult);
            
            if (Boolean.TRUE.equals(sendResult.get("success"))) {
                log.info("✅ [滞留通知] 微信模板消息发送成功{} - openid: {}, msgid: {}", 
                    userType, openid, sendResult.get("msgid"));
                return true;
            } else {
                log.warn("❌ [滞留通知] 微信模板消息发送失败{} - openid: {}, 错误码: {}, 错误信息: {}, 完整结果: {}",
                    userType, openid, sendResult.get("errcode"), sendResult.get("message"), sendResult);
                return false;
            }
        } catch (Exception e) {
            log.error("❌ [滞留通知] 微信模板消息发送异常{} - openid: {}, 车牌: {}", 
                userType, openid, appointment.getPlatenumber(), e);
            return false;
        }
    }
    
    /**
     * 获取实际使用的推送时间段配置（用于日志显示）
     * @param parkCode 车场编码
     * @return 时间段数组 [开始时间, 结束时间]，如 ["05:00", "21:00"]
     */
    private String[] getNotificationTimeRange(String parkCode) {
        String startTime = DEFAULT_NOTIFICATION_START_TIME;
        String endTime = DEFAULT_NOTIFICATION_END_TIME;
        
        try {
            MonthlyTicketTimeoutConfig config = monthlyTicketTimeoutConfigService.lambdaQuery()
                .eq(MonthlyTicketTimeoutConfig::getParkCode, parkCode)
                .eq(MonthlyTicketTimeoutConfig::getIsActive, true)
                .isNotNull(MonthlyTicketTimeoutConfig::getDescription)
                .last("LIMIT 1")
                .one();
            
            if (config != null && StringUtils.hasText(config.getDescription())) {
                try {
                    JSONObject descJson = JSON.parseObject(config.getDescription());
                    String configStartTime = descJson.getString("notificationStartTime");
                    String configEndTime = descJson.getString("notificationEndTime");
                    
                    if (StringUtils.hasText(configStartTime) && StringUtils.hasText(configEndTime)) {
                        startTime = configStartTime;
                        endTime = configEndTime;
                    }
                } catch (Exception ignored) {
                    // 使用默认值
                }
            }
        } catch (Exception e) {
            // 使用默认值
        }
        
        return new String[]{startTime, endTime};
    }
}
