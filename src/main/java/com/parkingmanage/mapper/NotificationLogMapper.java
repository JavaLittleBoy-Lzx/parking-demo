package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.NotificationLog;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息发送记录 Mapper 接口
 * 
 * @author System
 * @since 2024-12-04
 */
public interface NotificationLogMapper extends BaseMapper<NotificationLog> {

    /**
     * 检查是否已发送过（用于防止重复发送）
     * 
     * @param appointmentId 预约ID
     * @param notifyTimePoint 通知时间点（15/5/1分钟）
     * @param minutes 时间范围（分钟）
     * @return 发送记录列表（5分钟内的）
     */
    List<NotificationLog> checkDuplicateSend(
        @Param("appointmentId") Integer appointmentId,
        @Param("notifyTimePoint") Integer notifyTimePoint,
        @Param("minutes") Integer minutes
    );

    /**
     * 查询指定预约的所有通知记录
     * 
     * @param appointmentId 预约ID
     * @return 通知记录列表
     */
    List<NotificationLog> getByAppointmentId(@Param("appointmentId") Integer appointmentId);

    /**
     * 查询指定车牌的通知历史
     * 
     * @param plateNumber 车牌号
     * @param limit 限制数量
     * @return 通知记录列表
     */
    List<NotificationLog> getByPlateNumber(
        @Param("plateNumber") String plateNumber,
        @Param("limit") Integer limit
    );

    /**
     * 查询发送失败的记录
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 失败记录列表
     */
    List<NotificationLog> getFailedNotifications(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计发送情况（按类型）
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果列表
     */
    List<Map<String, Object>> getStatisticsByType(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计发送情况（按日期）
     * 
     * @param days 天数（最近N天）
     * @return 统计结果列表
     */
    List<Map<String, Object>> getStatisticsByDate(@Param("days") Integer days);

    /**
     * 清理过期记录
     * 
     * @param beforeDate 在此日期之前的记录将被删除
     * @return 删除的记录数
     */
    int cleanExpiredLogs(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * 获取最近的发送记录
     * 
     * @param appointmentId 预约ID
     * @param notificationType 通知类型
     * @return 最近的发送记录
     */
    NotificationLog getLatestByAppointmentAndType(
        @Param("appointmentId") Integer appointmentId,
        @Param("notificationType") String notificationType
    );

    /**
     * 统计指定时间范围内的发送成功率
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 成功率（百分比）
     */
    Double getSuccessRate(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 查询需要重试的失败记录
     * 
     * @param maxRetryCount 最大重试次数
     * @param limit 限制数量
     * @return 需要重试的记录列表
     */
    List<NotificationLog> getRetryableFailedNotifications(
        @Param("maxRetryCount") Integer maxRetryCount,
        @Param("limit") Integer limit
    );
}
