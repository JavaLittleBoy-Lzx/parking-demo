package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.NotificationLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息发送记录 Service接口
 * 
 * @author System
 * @since 2024-12-04
 */
public interface NotificationLogService extends IService<NotificationLog> {

    /**
     * 记录发送成功
     * 
     * @param log 发送记录
     * @return 是否记录成功
     */
    boolean logSuccess(NotificationLog log);

    /**
     * 记录发送失败
     * 
     * @param log 发送记录
     * @return 是否记录成功
     */
    boolean logFailure(NotificationLog log);

    /**
     * 记录跳过（重复）
     * 
     * @param log 发送记录
     * @return 是否记录成功
     */
    boolean logSkipped(NotificationLog log);

    /**
     * 检查是否已发送过（防止重复）
     * 
     * @param appointmentId 预约ID
     * @param notifyTimePoint 通知时间点（15/5/1分钟）
     * @param minutes 时间范围（分钟，默认5分钟）
     * @return true=已发送，false=未发送
     */
    boolean isDuplicateSend(Integer appointmentId, Integer notifyTimePoint, Integer minutes);

    /**
     * 查询指定预约的所有通知记录
     * 
     * @param appointmentId 预约ID
     * @return 通知记录列表
     */
    List<NotificationLog> getByAppointmentId(Integer appointmentId);

    /**
     * 查询指定车牌的通知历史
     * 
     * @param plateNumber 车牌号
     * @param limit 限制数量（默认100）
     * @return 通知记录列表
     */
    List<NotificationLog> getByPlateNumber(String plateNumber, Integer limit);

    /**
     * 查询发送失败的记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 失败记录列表
     */
    List<NotificationLog> getFailedNotifications(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计发送情况（按类型）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    List<Map<String, Object>> getStatisticsByType(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计发送情况（按日期）
     * 
     * @param days 天数（最近N天）
     * @return 统计结果
     */
    List<Map<String, Object>> getStatisticsByDate(Integer days);

    /**
     * 清理过期记录
     * 
     * @param days 保留天数（删除N天前的记录）
     * @return 删除的记录数
     */
    int cleanExpiredLogs(Integer days);

    /**
     * 获取发送成功率
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 成功率（百分比）
     */
    Double getSuccessRate(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 创建超时通知记录
     * 
     * @param appointmentId 预约ID
     * @param plateNumber 车牌号
     * @param openid 接收者openid
     * @param notifyTimePoint 通知时间点（15/5/1）
     * @param remainingMinutes 剩余时间
     * @param receiverType 接收者类型
     * @param appointType 预约类型
     * @return 通知记录对象
     */
    NotificationLog createTimeoutLog(
        Integer appointmentId,
        String plateNumber,
        String openid,
        Integer notifyTimePoint,
        Integer remainingMinutes,
        String receiverType,
        String appointType
    );

    /**
     * 创建违规通知记录
     * 
     * @param appointmentId 预约ID
     * @param plateNumber 车牌号
     * @param openid 接收者openid
     * @param violationLocation 违规地点
     * @param parkingMinutes 停车时长
     * @param receiverType 接收者类型
     * @param appointType 预约类型
     * @return 通知记录对象
     */
    NotificationLog createViolationLog(
        Integer appointmentId,
        String plateNumber,
        String openid,
        String violationLocation,
        Integer parkingMinutes,
        String receiverType,
        String appointType
    );

    /**
     * 创建进场通知记录
     * 
     * @param appointmentId 预约ID
     * @param plateNumber 车牌号
     * @param openid 接收者openid
     * @param parkName 停车场名称
     * @param entryTime 进场时间
     * @param receiverType 接收者类型
     * @return 通知记录对象
     */
    NotificationLog createEntryLog(
        Integer appointmentId,
        String plateNumber,
        String openid,
        String parkName,
        LocalDateTime entryTime,
        String receiverType
    );

    /**
     * 创建出场通知记录
     * 
     * @param appointmentId 预约ID
     * @param plateNumber 车牌号
     * @param openid 接收者openid
     * @param parkName 停车场名称
     * @param entryTime 进场时间
     * @param exitTime 出场时间
     * @param parkingDuration 停车时长文本
     * @param receiverType 接收者类型
     * @return 通知记录对象
     */
    NotificationLog createExitLog(
        Integer appointmentId,
        String plateNumber,
        String openid,
        String parkName,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        String parkingDuration,
        String receiverType
    );

    /**
     * 创建加入黑名单通知记录
     * 
     * @param plateNumber 车牌号
     * @param openid 接收者openid
     * @param blacklistReason 加入黑名单原因
     * @param blacklistDays 黑名单有效天数
     * @param receiverType 接收者类型
     * @return 通知记录对象
     */
    NotificationLog createBlacklistAddLog(
        String plateNumber,
        String openid,
        String blacklistReason,
        Integer blacklistDays,
        String receiverType
    );

    /**
     * 创建预约成功通知记录
     * 
     * @param appointmentId 预约ID
     * @param plateNumber 车牌号
     * @param openid 接收者openid
     * @param parkName 停车场名称
     * @param visitDate 预约访问日期
     * @param receiverType 接收者类型
     * @param appointType 预约类型
     * @return 通知记录对象
     */
    NotificationLog createAppointmentSuccessLog(
        Integer appointmentId,
        String plateNumber,
        String openid,
        String parkName,
        String visitDate,
        String receiverType,
        String appointType
    );

    /**
     * 创建待审核提醒通知记录
     * 
     * @param appointmentId 预约ID
     * @param plateNumber 车牌号
     * @param openid 接收者openid
     * @param parkName 停车场名称
     * @param visitDate 预约访问日期
     * @param receiverType 接收者类型
     * @return 通知记录对象
     */
    NotificationLog createAppointmentPendingLog(
        Integer appointmentId,
        String plateNumber,
        String openid,
        String parkName,
        String visitDate,
        String receiverType
    );

    /**
     * 创建审核结果通知记录
     * 
     * @param appointmentId 预约ID
     * @param plateNumber 车牌号
     * @param openid 接收者openid
     * @param parkName 停车场名称
     * @param visitDate 预约访问日期
     * @param auditStatus 审核状态（通过/拒绝）
     * @param auditReason 审核原因/拒绝原因
     * @param receiverType 接收者类型
     * @return 通知记录对象
     */
    NotificationLog createAppointmentAuditLog(
        Integer appointmentId,
        String plateNumber,
        String openid,
        String parkName,
        String visitDate,
        String auditStatus,
        String auditReason,
        String receiverType
    );
}
