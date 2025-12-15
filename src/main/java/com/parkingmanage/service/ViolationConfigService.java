package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.ViolationConfig;

/**
 * 违规配置 服务类
 *
 * @author System
 */
public interface ViolationConfigService extends IService<ViolationConfig> {

    /**
     * 保存或更新违规自动拉黑配置
     *
     * @param parkName 车场名称
     * @param parkCode 车场编码
     * @param configType 配置类型
     * @param maxViolationCount 最大违规次数
     * @param blacklistType 黑名单类型
     * @param isPermanent 是否永久拉黑
     * @param blacklistValidDays 临时拉黑有效天数（从最后一次违规时间开始计算）
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    boolean saveOrUpdateConfig(String parkName, String parkCode, String configType,
                               Integer maxViolationCount, String blacklistType,
                               Boolean isPermanent, Integer blacklistValidDays,
                               String operatorId,Integer reminderIntervalMinutes);

    /**
     * 根据车场名称和配置类型获取配置
     *
     * @param parkName 车场名称
     * @param configType 配置类型
     * @return 配置信息
     */
    ViolationConfig getByParkNameAndType(String parkName, String configType);

    /**
     * 根据车场编码和配置类型获取配置
     *
     * @param parkCode 车场编码
     * @param configType 配置类型
     * @return 配置信息
     */
    ViolationConfig getByParkCodeAndType(String parkCode, String configType);

    /**
     * 删除配置
     *
     * @param parkName 车场名称
     * @param configType 配置类型
     * @return 是否成功
     */
    boolean deleteByParkNameAndType(String parkName, String configType);

    /**
     * 获取违规提醒最小发送间隔（分钟），若未配置则返回默认值
     * @param parkName 车场名称（传入 "GLOBAL" 读取全局配置）
     * @param defaultMinutes 默认分钟数
     * @return 分钟数
     */
    int getReminderIntervalMinutes(String parkName, int defaultMinutes);

    /**
     * 更新违规提醒最小发送间隔（分钟），使用 config_type=VIOLATION_REMINDER
     * @param parkName 车场名称（使用 "GLOBAL" 表示全局配置）
     * @param minutes 分钟数
     * @param operatorId 操作人
     * @return 是否成功
     */
    boolean updateReminderIntervalMinutes(String parkName, int minutes, String operatorId);
}

