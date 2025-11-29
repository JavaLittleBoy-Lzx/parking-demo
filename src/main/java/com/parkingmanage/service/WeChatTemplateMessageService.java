package com.parkingmanage.service;

import java.util.Map;

/**
 * 微信模板消息服务接口
 */
public interface WeChatTemplateMessageService {
    
    /**
     * 发送模板消息
     * @param openid 接收者openid
     * @param templateId 模板ID
     * @param data 模板数据
     * @param url 跳转链接（可选）
     * @return 发送结果
     */
    Map<String, Object> sendTemplateMessage(String openid, String templateId, Map<String, Object> data, String url);
    
    /**
     * 给管家发送停车进场通知
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @param enterTime 进场时间
     * @param managerNickname 管家昵称
     * @return 发送结果
     */
    Map<String, Object> sendParkingEnterNotification(String plateNumber, String parkName, String enterChannel, String enterTime, String managerNickname);
    
    /**
     * 给管家发送停车离场通知
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @param leaveTime 离场时间
     * @param managerNickname 管家昵称
     * @return 发送结果
     */
    Map<String, Object> sendParkingLeaveNotification(String plateNumber, String parkName, String leaveTime, String enterTime,String managerNickname,String leaveChannel);
    
    /**
     * 给管家发送停车超时通知
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @param enterTime 进场时间
     * @param managerNickname 管家昵称
     * @param overtimeMinutes 超时分钟数
     * @return 发送结果
     */
    Map<String, Object> sendParkingTimeoutNotification(String plateNumber, String parkName, String enterTime, String managerNickname, long overtimeMinutes);
    
    /**
     * 🆕 给访客发送即将超时提醒（车辆还有15分钟就要超过2小时）
     * @param openid 访客的微信openid
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @param enterTime 进场时间
     * @param remainingMinutes 剩余时间（分钟）
     * @return 发送结果
     */
    Map<String, Object> sendParkingAlmostTimeoutNotification(String openid, String plateNumber, String parkName, String enterTime, long remainingMinutes);
    
    /**
     * 给管家发送车辆违规停车告警通知
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @param violationLocation 违规地点
     * @param parkingDuration 停车时长
     * @param managerNickname 管家昵称
     * @return 发送结果
     */
    Map<String, Object> sendParkingViolationNotification(String plateNumber, String parkName, String violationLocation, String parkingDuration, String managerNickname);
    
    /**
     * 给管家发送预约车辆待审核提醒
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @param contactPhone 联系电话
     * @param managerNickname 管家昵称
     * @return 发送结果
     */
    Map<String, Object> sendBookingPendingNotification(String plateNumber, String parkName, String contactPhone, String managerNickname);
    
    /**
     * 给访客发送预约审核结果通知
     * @param plateNumber 车牌号
     * @param parkName 停车场名称  
     * @param auditResult 审核结果（已通过/未通过）
     * @param auditReason 审核备注（通过时为空，驳回时为驳回原因）
     * @param appointmentTime 预约时间
     * @param visitorName 访客姓名（用于查询openid）
     * @param managerName 审核管家姓名
     * @return 发送结果
     */
    Map<String, Object> sendAppointmentAuditResultNotification(String plateNumber, String parkName,
            String auditResult, String auditReason, String appointmentTime, String visitorName, String managerName);
} 