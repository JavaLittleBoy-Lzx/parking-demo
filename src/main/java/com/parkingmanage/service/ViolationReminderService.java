package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.ViolationReminder;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 违规提醒记录表 服务类
 * </p>
 *
 * @author parking-system
 * @since 2024-01-XX
 */
public interface ViolationReminderService extends IService<ViolationReminder> {

    /**
     * 根据车牌号查询未处理的违规提醒
     */
    List<ViolationReminder> getUnprocessedByPlateNumber(String plateNumber);

    /**
     * 根据车牌号标记所有未处理的提醒为已处理
     */
    boolean markAllAsProcessedByPlateNumber(String plateNumber, String processedBy);

    /**
     * 根据车牌号查询所有违规提醒记录
     */
    List<ViolationReminder> getAllByPlateNumber(String plateNumber);

    /**
     * 查询指定时间范围内的违规提醒记录
     */
    List<ViolationReminder> getByTimeRange(String startTime, String endTime);

    /**
     * 统计未处理的违规提醒数量
     */
    int countUnprocessedReminders();

    /**
     * 统计指定车牌的违规提醒次数
     */
    int countByPlateNumber(String plateNumber);

    /**
     * 创建违规提醒记录
     */
    boolean createViolationReminder(ViolationReminder reminder);

    /**
     * 检查是否需要发送违规提醒（第一次违规）
     */
    boolean shouldSendReminder(String plateNumber);

    /**
     * 检查是否需要发送违规短信（第二次及以后违规）
     */
    boolean shouldSendViolationSms(String plateNumber);

    /**
     * 处理单个违规提醒记录（标记为已处理）
     */
    boolean processViolationReminder(Long id, String processedBy);

    // ==================== 📊 统计分析接口 ====================

    /**
     * 违规记录与提醒发送关联分析
     * @param days 统计天数
     * @return 统计结果
     */
    List<Map<String, Object>> getCorrelationAnalysis(Integer days);
}
