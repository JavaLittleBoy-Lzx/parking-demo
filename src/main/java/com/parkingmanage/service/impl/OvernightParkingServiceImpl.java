package com.parkingmanage.service.impl;

import com.parkingmanage.service.OvernightParkingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 过夜停车判定服务实现
 * 
 * @author parkingmanage
 * @since 2025-09-19
 */
@Slf4j
@Service
public class OvernightParkingServiceImpl implements OvernightParkingService {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    @Override
    public boolean isOvernightViolation(LocalDateTime enterTime, LocalDateTime leaveTime,
                                      String nightStartTime, String nightEndTime, int nightTimeHours) {
        
        OvernightParkingAnalysis analysis = analyzeOvernightParking(enterTime, leaveTime, 
                                                                  nightStartTime, nightEndTime, nightTimeHours);
        return analysis.isViolation();
    }
    
    @Override
    public double calculateNightParkingHours(LocalDateTime enterTime, LocalDateTime leaveTime,
                                           String nightStartTime, String nightEndTime) {
        
        if (enterTime == null) {
            log.warn("⚠️ [过夜计算] 进场时间为null");
            return 0.0;
        }
        
        // 如果离场时间为null，使用当前时间
        LocalDateTime actualLeaveTime = leaveTime != null ? leaveTime : LocalDateTime.now();
        
        try {
            LocalTime nightStart = LocalTime.parse(nightStartTime, TIME_FORMATTER);
            LocalTime nightEnd = LocalTime.parse(nightEndTime, TIME_FORMATTER);
            
            log.info("🌙 [夜间时段] {}:{} - {}:{}", 
                    nightStart.getHour(), nightStart.getMinute(),
                    nightEnd.getHour(), nightEnd.getMinute());
            
            double totalNightHours = 0.0;
            
            // 从进场日期开始，逐日计算夜间时段的重叠时间
            LocalDateTime currentDate = enterTime.toLocalDate().atStartOfDay();
            LocalDateTime endDate = actualLeaveTime.toLocalDate().plusDays(1).atStartOfDay();
            
            while (currentDate.isBefore(endDate)) {
                // 计算当天夜间时段与停车时间的重叠
                double dayNightHours = calculateDayNightOverlap(enterTime, actualLeaveTime, 
                                                              currentDate, nightStart, nightEnd);
                totalNightHours += dayNightHours;
                
                log.debug("📅 [{}] 当日夜间重叠: {}小时", 
                         currentDate.toLocalDate(), dayNightHours);
                
                currentDate = currentDate.plusDays(1);
            }
            
            log.info("🕐 [夜间总时长] {}小时", totalNightHours);
            return totalNightHours;
            
        } catch (Exception e) {
            log.error("❌ [夜间时长计算失败] error={}", e.getMessage(), e);
            return 0.0;
        }
    }
    
    @Override
    public OvernightParkingAnalysis analyzeOvernightParking(LocalDateTime enterTime, LocalDateTime leaveTime,
                                                          String nightStartTime, String nightEndTime, int nightTimeHours) {
        
        if (enterTime == null) {
            return new OvernightParkingAnalysis(false, 0.0, 0.0, null, "进场时间为空");
        }
        
        // 如果离场时间为null，使用当前时间
        LocalDateTime actualLeaveTime = leaveTime != null ? leaveTime : LocalDateTime.now();
        
        // 计算总停车时长
        Duration totalDuration = Duration.between(enterTime, actualLeaveTime);
        double totalHours = totalDuration.toMinutes() / 60.0;
        
        // 计算夜间时段停车时长
        double nightHours = calculateNightParkingHours(enterTime, actualLeaveTime, nightStartTime, nightEndTime);
        
        // 判断是否违规
        boolean isViolation = nightHours > nightTimeHours;
        
        String reason = null;
        String description;
        
        if (isViolation) {
            reason = String.format("夜间时段(%s-%s)停车%.1f小时，超过限制%d小时", 
                                 nightStartTime, nightEndTime, nightHours, nightTimeHours);
            description = String.format("过夜违规：总停车%.1f小时，夜间时段停车%.1f小时，超过限制%d小时", 
                                       totalHours, nightHours, nightTimeHours);
        } else {
            description = String.format("正常停车：总停车%.1f小时，夜间时段停车%.1f小时，未超过限制%d小时", 
                                       totalHours, nightHours, nightTimeHours);
        }
        
        log.info("🔍 [过夜分析] 车辆停车分析 - 总时长: {}小时, 夜间时长: {}小时, 违规: {}", 
                totalHours, nightHours, isViolation);
        
        return new OvernightParkingAnalysis(isViolation, totalHours, nightHours, reason, description);
    }
    
    /**
     * 计算某一天夜间时段与停车时间的重叠时长
     * 
     * @param enterTime 进场时间
     * @param leaveTime 离场时间
     * @param currentDate 当前计算的日期（当天00:00:00）
     * @param nightStart 夜间开始时间
     * @param nightEnd 夜间结束时间
     * @return 当天夜间重叠的小时数
     */
    private double calculateDayNightOverlap(LocalDateTime enterTime, LocalDateTime leaveTime,
                                          LocalDateTime currentDate, LocalTime nightStart, LocalTime nightEnd) {
        
        // 夜间时段可能跨日期，需要分两段处理
        double overlapHours = 0.0;
        
        // 第一段：当天夜间开始时间到午夜
        LocalDateTime todayNightStart = currentDate.with(nightStart);
        LocalDateTime todayMidnight = currentDate.plusDays(1).withHour(0).withMinute(0).withSecond(0);
        
        double segment1 = calculateTimeOverlap(enterTime, leaveTime, todayNightStart, todayMidnight);
        overlapHours += segment1;
        
        // 第二段：午夜到次日夜间结束时间
        LocalDateTime tomorrowMidnight = currentDate.plusDays(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrowNightEnd = currentDate.plusDays(1).with(nightEnd);
        
        double segment2 = calculateTimeOverlap(enterTime, leaveTime, tomorrowMidnight, tomorrowNightEnd);
        overlapHours += segment2;
        
        log.debug("📊 [夜间重叠] 日期: {}, 第一段: {}h, 第二段: {}h, 总计: {}h", 
                 currentDate.toLocalDate(), segment1, segment2, overlapHours);
        
        return overlapHours;
    }
    
    /**
     * 计算两个时间段的重叠时长
     * 
     * @param start1 时间段1开始
     * @param end1 时间段1结束
     * @param start2 时间段2开始
     * @param end2 时间段2结束
     * @return 重叠的小时数
     */
    private double calculateTimeOverlap(LocalDateTime start1, LocalDateTime end1,
                                      LocalDateTime start2, LocalDateTime end2) {
        
        // 计算重叠区间
        LocalDateTime overlapStart = start1.isAfter(start2) ? start1 : start2;
        LocalDateTime overlapEnd = end1.isBefore(end2) ? end1 : end2;
        
        // 如果没有重叠，返回0
        if (overlapStart.isAfter(overlapEnd) || overlapStart.equals(overlapEnd)) {
            return 0.0;
        }
        
        // 计算重叠时长（小时）
        Duration overlapDuration = Duration.between(overlapStart, overlapEnd);
        double overlapHours = overlapDuration.toMinutes() / 60.0;
        
        log.debug("⏱️ [时间重叠] {}至{} 与 {}至{} 重叠 {}小时", 
                 start1.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                 end1.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                 start2.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                 end2.format(DateTimeFormatter.ofPattern("MM-dd HH:mm")),
                 overlapHours);
        
        return overlapHours;
    }
    
    /**
     * 格式化时间显示
     */
    private String formatTime(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
} 