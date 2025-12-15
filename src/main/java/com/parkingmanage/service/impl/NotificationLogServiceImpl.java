package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.NotificationLog;
import com.parkingmanage.mapper.NotificationLogMapper;
import com.parkingmanage.service.NotificationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息发送记录 Service实现类
 * 
 * @author System
 * @since 2024-12-04
 */
@Slf4j
@Service
public class NotificationLogServiceImpl extends ServiceImpl<NotificationLogMapper, NotificationLog> 
        implements NotificationLogService {

    @Override
    public boolean logSuccess(NotificationLog notificationLog) {
        try {
            notificationLog.setSendStatus(NotificationLog.SendStatus.SUCCESS);
            notificationLog.setSuccess(1);
            notificationLog.setSendTime(LocalDateTime.now());
            return save(notificationLog);
        } catch (Exception e) {
            log.error("❌ 记录发送成功日志异常", e);
            return false;
        }
    }

    @Override
    public boolean logFailure(NotificationLog notificationLog) {
        try {
            notificationLog.setSendStatus(NotificationLog.SendStatus.FAILED);
            notificationLog.setSuccess(0);
            notificationLog.setSendTime(LocalDateTime.now());
            return save(notificationLog);
        } catch (Exception e) {
            log.error("❌ 记录发送失败日志异常", e);
            return false;
        }
    }

    @Override
    public boolean logSkipped(NotificationLog notificationLog) {
        try {
            notificationLog.setSendStatus(NotificationLog.SendStatus.SKIPPED);
            notificationLog.setSuccess(0);
            notificationLog.setSendTime(LocalDateTime.now());
            notificationLog.setErrorMessage("重复发送，已跳过");
            return save(notificationLog);
        } catch (Exception e) {
            log.error("❌ 记录跳过日志异常", e);
            return false;
        }
    }

    @Override
    public boolean isDuplicateSend(Integer appointmentId, Integer notifyTimePoint, Integer minutes) {
        try {
            if (minutes == null) {
                minutes = 5; // 默认5分钟
            }
            
            List<NotificationLog> logs = baseMapper.checkDuplicateSend(
                appointmentId, notifyTimePoint, minutes
            );
            
            return logs != null && !logs.isEmpty();
        } catch (Exception e) {
            log.error("❌ 检查重复发送异常 - appointmentId: {}, notifyTimePoint: {}", 
                appointmentId, notifyTimePoint, e);
            return false;
        }
    }

    @Override
    public List<NotificationLog> getByAppointmentId(Integer appointmentId) {
        return baseMapper.getByAppointmentId(appointmentId);
    }

    @Override
    public List<NotificationLog> getByPlateNumber(String plateNumber, Integer limit) {
        if (limit == null) {
            limit = 100; // 默认100条
        }
        return baseMapper.getByPlateNumber(plateNumber, limit);
    }

    @Override
    public List<NotificationLog> getFailedNotifications(LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.getFailedNotifications(startTime, endTime);
    }

    @Override
    public List<Map<String, Object>> getStatisticsByType(LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.getStatisticsByType(startTime, endTime);
    }

    @Override
    public List<Map<String, Object>> getStatisticsByDate(Integer days) {
        if (days == null) {
            days = 7; // 默认最近7天
        }
        return baseMapper.getStatisticsByDate(days);
    }

    @Override
    public int cleanExpiredLogs(Integer days) {
        if (days == null) {
            days = 90; // 默认保留90天
        }
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(days);
        int count = baseMapper.cleanExpiredLogs(beforeDate);
        log.info("🧹 清理过期通知日志 - 删除{}天前的记录，共删除: {}条", days, count);
        return count;
    }

    @Override
    public Double getSuccessRate(LocalDateTime startTime, LocalDateTime endTime) {
        return baseMapper.getSuccessRate(startTime, endTime);
    }

    @Override
    public NotificationLog createTimeoutLog(
            Integer appointmentId,
            String plateNumber,
            String openid,
            Integer notifyTimePoint,
            Integer remainingMinutes,
            String receiverType,
            String appointType) {
        
        NotificationLog log = new NotificationLog();
        log.setAppointmentId(appointmentId);
        log.setPlateNumber(plateNumber);
        log.setOpenid(openid);
        
        // 设置通知类型
        String notificationType;
        if (notifyTimePoint == 15) {
            notificationType = NotificationLog.NotificationType.TIMEOUT_15MIN;
        } else if (notifyTimePoint == 5) {
            notificationType = NotificationLog.NotificationType.TIMEOUT_5MIN;
        } else if (notifyTimePoint == 1) {
            notificationType = NotificationLog.NotificationType.TIMEOUT_1MIN;
        } else {
            notificationType = "timeout_" + notifyTimePoint + "min";
        }
        log.setNotificationType(notificationType);
        
        log.setNotifyTimePoint(notifyTimePoint);
        log.setRemainingMinutes(remainingMinutes);
        log.setReceiverType(receiverType);
        log.setAppointType(appointType);
        log.setSendBy(NotificationLog.SendBy.SCHEDULED_TASK);
        log.setRetryCount(0);
        
        return log;
    }

    @Override
    public NotificationLog createViolationLog(
            Integer appointmentId,
            String plateNumber,
            String openid,
            String violationLocation,
            Integer parkingMinutes,
            String receiverType,
            String appointType) {
        
        NotificationLog log = new NotificationLog();
        log.setAppointmentId(appointmentId);
        log.setPlateNumber(plateNumber);
        log.setOpenid(openid);
        log.setNotificationType(NotificationLog.NotificationType.VIOLATION);
        log.setViolationLocation(violationLocation);
        log.setParkingMinutes(parkingMinutes);
        log.setReceiverType(receiverType);
        log.setAppointType(appointType);
        log.setSendBy(NotificationLog.SendBy.SCHEDULED_TASK);
        log.setRetryCount(0);
        
        return log;
    }

    @Override
    public NotificationLog createEntryLog(
            Integer appointmentId,
            String plateNumber,
            String openid,
            String parkName,
            LocalDateTime entryTime,
            String receiverType) {
        
        NotificationLog log = new NotificationLog();
        log.setAppointmentId(appointmentId);
        log.setPlateNumber(plateNumber);
        log.setOpenid(openid);
        log.setNotificationType(NotificationLog.NotificationType.ENTRY);
        log.setParkName(parkName);
        log.setEntryTime(entryTime);
        log.setReceiverType(receiverType);
        log.setSendBy(NotificationLog.SendBy.API);
        log.setRetryCount(0);
        
        return log;
    }

    @Override
    public NotificationLog createExitLog(
            Integer appointmentId,
            String plateNumber,
            String openid,
            String parkName,
            LocalDateTime entryTime,
            LocalDateTime exitTime,
            String parkingDuration,
            String receiverType) {
        
        NotificationLog log = new NotificationLog();
        log.setAppointmentId(appointmentId);
        log.setPlateNumber(plateNumber);
        log.setOpenid(openid);
        log.setNotificationType(NotificationLog.NotificationType.EXIT);
        log.setParkName(parkName);
        log.setEntryTime(entryTime);
        log.setExitTime(exitTime);
        log.setParkingDuration(parkingDuration);
        log.setReceiverType(receiverType);
        log.setSendBy(NotificationLog.SendBy.API);
        log.setRetryCount(0);
        
        return log;
    }

    @Override
    public NotificationLog createBlacklistAddLog(
            String plateNumber,
            String openid,
            String blacklistReason,
            Integer blacklistDays,
            String receiverType) {
        
        NotificationLog log = new NotificationLog();
        log.setPlateNumber(plateNumber);
        log.setOpenid(openid);
        log.setNotificationType(NotificationLog.NotificationType.BLACKLIST_ADD);
        log.setBlacklistReason(blacklistReason);
        log.setBlacklistDays(blacklistDays);
        log.setReceiverType(receiverType);
        log.setSendBy(NotificationLog.SendBy.API);
        log.setRetryCount(0);
        
        return log;
    }

    @Override
    public NotificationLog createAppointmentSuccessLog(
            Integer appointmentId,
            String plateNumber,
            String openid,
            String parkName,
            String visitDate,
            String receiverType,
            String appointType) {
        
        NotificationLog log = new NotificationLog();
        log.setAppointmentId(appointmentId);
        log.setPlateNumber(plateNumber);
        log.setOpenid(openid);
        log.setNotificationType(NotificationLog.NotificationType.APPOINTMENT_SUCCESS);
        log.setParkName(parkName);
        log.setVisitDate(visitDate);
        log.setReceiverType(receiverType);
        log.setAppointType(appointType);
        log.setSendBy(NotificationLog.SendBy.API);
        log.setRetryCount(0);
        
        return log;
    }

    @Override
    public NotificationLog createAppointmentPendingLog(
            Integer appointmentId,
            String plateNumber,
            String openid,
            String parkName,
            String visitDate,
            String receiverType) {
        
        NotificationLog log = new NotificationLog();
        log.setAppointmentId(appointmentId);
        log.setPlateNumber(plateNumber);
        log.setOpenid(openid);
        log.setNotificationType(NotificationLog.NotificationType.APPOINTMENT_PENDING);
        log.setParkName(parkName);
        log.setVisitDate(visitDate);
        log.setReceiverType(receiverType);
        log.setSendBy(NotificationLog.SendBy.API);
        log.setRetryCount(0);
        
        return log;
    }

    @Override
    public NotificationLog createAppointmentAuditLog(
            Integer appointmentId,
            String plateNumber,
            String openid,
            String parkName,
            String visitDate,
            String auditStatus,
            String auditReason,
            String receiverType) {
        
        NotificationLog log = new NotificationLog();
        log.setAppointmentId(appointmentId);
        log.setPlateNumber(plateNumber);
        log.setOpenid(openid);
        log.setNotificationType(NotificationLog.NotificationType.APPOINTMENT_AUDIT);
        log.setParkName(parkName);
        log.setVisitDate(visitDate);
        log.setAuditStatus(auditStatus);
        log.setAuditReason(auditReason);
        log.setReceiverType(receiverType);
        log.setSendBy(NotificationLog.SendBy.API);
        log.setRetryCount(0);
        
        return log;
    }
}
