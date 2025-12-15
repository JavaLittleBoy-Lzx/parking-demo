package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.MonthlyTicketTimeoutConfig;

/**
 * 月票车超时配置 服务类
 *
 * @author System
 */
public interface MonthlyTicketTimeoutConfigService extends IService<MonthlyTicketTimeoutConfig> {

    /**
     * 保存或更新月票车超时配置
     *
     * @param parkCode 车场编码
     * @param parkName 车场名称  
     * @param timeoutMinutes 超时时间（分钟）
     * @param maxViolationCount 最大违规次数
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    boolean saveOrUpdateConfig(String parkCode, String parkName, Integer timeoutMinutes, Integer maxViolationCount, String operatorId);

    /**
     * 根据车场编码获取配置
     *
     * @param parkCode 车场编码
     * @return 配置信息
     */
    MonthlyTicketTimeoutConfig getByParkCode(String parkCode);

    /**
     * 删除配置
     *
     * @param parkCode 车场编码
     * @return 是否成功
     */
    boolean deleteByParkCode(String parkCode);
    
    /**
     * 保存或更新完整配置（包含过夜停车配置）
     *
     * @param parkCode 车场编码
     * @param parkName 车场名称
     * @param timeoutMinutes 超时时间（分钟）
     * @param maxViolationCount 最大违规次数
     * @param nightStartTime 夜间开始时间
     * @param nightEndTime 夜间结束时间
     * @param nightTimeHours 夜间时段超时小时数
     * @param enableOvernightCheck 是否启用过夜检查
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    boolean saveOrUpdateFullConfig(String parkCode, String parkName, Integer timeoutMinutes, 
                                  Integer maxViolationCount, String nightStartTime, String nightEndTime,
                                  Integer nightTimeHours, Boolean enableOvernightCheck, String operatorId);
    
    /**
     * 🆕 保存或更新过夜配置（新版过夜规则）
     *
     * @param parkCode 车场编码
     * @param parkName 车场名称
     * @param timeoutMinutes 超时时间（分钟）
     * @param maxViolationCount 最大违规次数
     * @param overnightTimeHours 过夜判定时长（小时）
     * @param enableOvernightCheck 是否启用过夜检查
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    boolean saveOrUpdateOvernightConfig(String parkCode, String parkName, Integer timeoutMinutes, 
                                       Integer maxViolationCount, Integer overnightTimeHours, 
                                       Boolean enableOvernightCheck, String operatorId);
} 