package com.parkingmanage.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.Violations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 违规记录表 服务类
 * </p>
 *
 * @author MLH
 * @since 2025-01-31
 */
public interface ViolationsService extends IService<Violations> {

    /**
     * 创建违规记录
     */
    boolean createViolation(Violations violation);

    /**
     * 分页查询违规记录（支持创建者权限过滤和小区过滤）
     */
    IPage<Map<String, Object>> getViolationsWithOwnerInfo(
            Page<Map<String, Object>> page,
            String plateNumber,
            String status,
            String violationType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String createdByFilter,
            String communityFilter,
            Boolean useDirectQuery
    );

    /**
     * 更新违规记录状态
     */
    boolean updateViolationStatus(Long id, String status, String remark, Integer handlerId);

    /**
     * 检查用户是否可以更新违规记录
     */
    boolean canUpdateViolation(Long violationId, String currentUserId, String userRole);

    /**
     * 删除违规记录
     * @param violationId 违规记录ID
     * @param currentUserId 当前操作用户ID
     * @return 是否删除成功
     */
    boolean deleteViolation(Long violationId, String currentUserId);

    /**
     * 获取高风险车辆列表（支持创建者和小区过滤）
     */
    List<Map<String, Object>> getHighRiskVehicles(LocalDateTime startDate, LocalDateTime endDate, Integer limit, String createdByFilter, String communityFilter);

    /**
     * 获取违规统计数据（支持创建者和小区过滤）
     */
    Map<String, Object> getViolationStatistics(LocalDateTime startDate, LocalDateTime endDate, String plateNumber, String createdByFilter, String communityFilter);

    /**
     * 根据车牌号查询车主信息
     */
    Map<String, Object> getOwnerByPlateNumber(String plateNumber);

    /**
     * 获取车牌号搜索建议
     */
    List<Map<String, Object>> getPlateSuggestions(String keyword, String usercode);

    /**
     * 🆕 从违规记录中获取车牌号搜索建议
     */
    List<Map<String, Object>> getViolationPlateSuggestions(String keyword, String parkCode);

    /**
     * 获取车主的车辆列表
     */
    List<Map<String, Object>> getOwnerVehicles(Integer ownerId);

    /**
     * 更新车主信用分
     */
    boolean updateOwnerCreditScore(Integer ownerId, Integer creditScore);

    /**
     * 根据车牌号查询预约记录（用于违规录入）
     */
    List<Map<String, Object>> getAppointmentRecordsByPlate(String plateNumber);

    /**
     * 🆕 根据预约ID查询预约详细信息（小程序端专用）
     */
    Map<String, Object> getAppointmentDetail(Integer appointmentId);

    /**
     * 根据车牌号分析违规情况
     */
    Map<String, Object> analyzeViolationByPlate(String plateNumber);

    /**
     * 🆕 通过业主信息关联查询预约记录
     * 关联ownerinfo表和appointment表，筛选与当前巡逻员相同车场的数据
     * 
     * @param keyword 搜索关键词（车牌号或业主姓名）
     * @param page 页码
     * @param size 每页数量
     * @param usercode 巡逻员编码
     * @return 预约记录列表
     */
    List<Map<String, Object>> getAppointmentRecordsByOwnerInfo(String keyword, Integer page, Integer size, String usercode);

    /**
     * 🆕 获取管家所在小区
     * @param userId 用户ID
     * @return 小区名称
     */
    String getButlerCommunity(String userId);

    /**
     * 🆕 根据车牌号、停车场编码和进场时间查询并更新离场时间
     * @param plateNumber 车牌号
     * @param parkCode 停车场编码
     * @param enterTime 进场时间（推送数据中的格式）
     * @param leaveTime 离场时间（推送数据中的格式）
     * @return 是否更新成功
     */
    boolean updateLeaveTimeByPlateAndTime(String plateNumber, String parkCode, String enterTime, String leaveTime);

    /**
     * 🆕 月票车超时配置管理
     * 获取月票车超时配置（超时时间和累计次数）
     * @param parkCode 停车场编码
     * @return 配置信息 Map，包含 timeoutMinutes 和 maxViolationCount
     */
    Map<String, Object> getMonthlyTicketTimeoutConfig(String parkCode);

    /**
     * 🆕 保存月票车超时配置
     * @param parkCode 停车场编码  
     * @param timeoutMinutes 超时时间（分钟）
     * @param maxViolationCount 最大违规次数
     * @param operatorId 操作员ID
     * @return 是否保存成功
     */
    boolean saveMonthlyTicketTimeoutConfig(String parkCode, Integer timeoutMinutes, Integer maxViolationCount, String operatorId);
    
    /**
     * 🆕 保存月票车完整配置（包含过夜停车配置）
     * @param parkCode 车场编码
     * @param timeoutMinutes 超时时间（分钟）
     * @param maxViolationCount 最大违规次数
     * @param nightStartTime 夜间开始时间
     * @param nightEndTime 夜间结束时间
     * @param nightTimeHours 夜间时段超时小时数
     * @param enableOvernightCheck 是否启用过夜检查
     * @param operatorId 操作员ID
     * @return 保存结果
     */
    boolean saveMonthlyTicketFullConfig(String parkCode, Integer timeoutMinutes, Integer maxViolationCount,
                                       String nightStartTime, String nightEndTime, Integer nightTimeHours,
                                       Boolean enableOvernightCheck, String operatorId);
    
    /**
     * 🆕 保存月票车完整配置（包含过夜停车配置和免检类型）
     * @param parkCode 车场编码
     * @param timeoutMinutes 超时时间（分钟）
     * @param maxViolationCount 最大违规次数
     * @param overnightTimeHours 过夜判定时长（小时）
     * @param enableOvernightCheck 是否启用过夜检查
     * @param exemptTicketTypes 免检的月票类型列表
     * @param operatorId 操作员ID
     * @return 保存结果
     */
    boolean saveMonthlyTicketFullConfigWithExempt(String parkCode, Integer timeoutMinutes, Integer maxViolationCount,
                                                 Integer overnightTimeHours, Boolean enableOvernightCheck, 
                                                 java.util.List<String> exemptTicketTypes, String operatorId);



    /**
     * 记录车辆违规（支持自定义违规类型和描述）
     * @param plateNumber 车牌号
     * @param parkCode 停车场代码
     * @param parkName 停车场名称
     * @param enterTime 进场时间
     * @param leaveTime 离场时间
     * @param parkingDurationMinutes 停车时长（分钟）
     * @param violationType 违规类型
     * @param violationDescription 违规描述
     * @param monthTicketId 月票ID（可为null）
     * @param shouldDirectBlacklist 是否应直接拉黑
     * @return 是否记录成功
     */
    boolean recordViolation(String plateNumber, String parkCode, String parkName, 
                          LocalDateTime enterTime, LocalDateTime leaveTime, 
                          Long parkingDurationMinutes, String violationType, String violationDescription,
                          Integer monthTicketId, boolean shouldDirectBlacklist);

    /**
     * 🆕 检查月票车违规次数并决定是否加入黑名单
     * @param plateNumber 车牌号
     * @param parkCode 停车场编码
     * @return 是否应该加入黑名单
     */
    boolean checkAndProcessBlacklist(String plateNumber, String parkCode);

    /**
     * 🆕 添加车辆到黑名单
     * @param plateNumber 车牌号
     * @param parkName 停车场名称
     * @param reason 黑名单原因
     * @param remark 备注
     * @return 是否添加成功
     */
    boolean addToBlacklist(String plateNumber, String parkName, String reason, String remark);

    /**
     * 🆕 检查是否存在重复的违规记录
     * @param plateNumber 车牌号
     * @param parkCode 停车场编码
     * @param enterTime 进场时间
     * @param leaveTime 离场时间
     * @return 是否存在重复记录
     */
    boolean checkDuplicateViolation(String plateNumber, String parkCode, LocalDateTime enterTime, LocalDateTime leaveTime);

    /**
     * 🆕 根据车牌号和停车场编码删除违规记录
     * @param plateNumber 车牌号
     * @param parkCode 停车场编码
     * @return 删除的记录数量
     */
    int deleteViolationsByPlateAndPark(String plateNumber, String parkCode);

    // ==================== 🆕 东北林业大学违规阈值配置接口 ====================
    
    /**
     * 获取东北林业大学违规阈值配置
     * @return 配置信息 {parkName, maxViolationCount, updateTime}
     */
    java.util.Map<String, Object> getNebuViolationConfig();
    
    /**
     * 保存东北林业大学违规阈值配置
     * @param parkName 停车场名称
     * @param maxViolationCount 最大违规次数
     * @param blacklistType 黑名单类型（格式：code|name）
     * @param isPermanent 是否永久拉黑
     * @param blacklistValidDays 拉黑有效天数（临时拉黑时使用，自动拉黑时从最后一次违规时间开始计算）
     * @return 是否保存成功
     */
    boolean saveNebuViolationConfig(String parkName, Integer maxViolationCount, 
                                   String blacklistType, Boolean isPermanent,
                                   Integer blacklistValidDays,Integer reminderIntervalMinutes);
    
    // ==================== 🆕 违规记录处理功能接口 ====================

    /**
     * 手动处理单条违规记录
     * @param violationId 违规记录ID
     * @param operatorName 操作员用户名
     * @param processRemark 处理备注
     * @return 是否处理成功
     */
    boolean manualProcessViolation(Long violationId, String operatorName, String processRemark);

    /**
     * 手动批量处理违规记录
     * @param violationIds 违规记录ID列表
     * @param operatorName 操作员用户名
     * @param processRemark 处理备注
     * @return 处理成功的数量
     */
    int batchProcessViolations(List<Long> violationIds, String operatorName, String processRemark);

    /**
     * 检查并执行自动拉黑（在创建违规记录时调用）
     * @param plateNumber 车牌号
     * @param parkCode 停车场编码
     * @return 是否触发了自动拉黑
     */
    boolean checkAndAutoBlacklist(String plateNumber, String parkCode);

    /**
     * 统计指定车牌的未处理违规次数
     * @param plateNumber 车牌号
     * @return 未处理违规次数
     */
    int countUnprocessedViolations(String plateNumber);

    /**
     * 分页查询违规记录（支持处理状态和处理方式筛选）
     * @param page 分页参数
     * @param plateNumber 车牌号
     * @param status 状态
     * @param violationType 违规类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param createdByFilter 创建者过滤
     * @param communityFilter 小区过滤
     * @param processStatus 处理状态筛选
     * @param processType 处理方式筛选
     * @param onlyUnprocessed 是否仅查询未处理
     * @return 违规记录分页数据
     */
    IPage<Map<String, Object>> getViolationsWithProcess(
            Page<Map<String, Object>> page,
            String plateNumber,
            String status,
            String violationType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String createdByFilter,
            String communityFilter,
            String processStatus,
            String processType,
            Boolean onlyUnprocessed
    );

    // ==================== 🆕 学院新城拉黑规则配置 ====================

    /**
     * 获取学院新城拉黑规则配置
     * @param parkCode 车场编码
     * @return 配置信息（可能包含默认值）
     */
    Map<String, Object> getCollegeNewCityConfig(String parkCode);

    /**
     * 保存学院新城拉黑规则配置
     * @param parkCode 车场编码
     * @param parkName 车场名称
     * @param blacklistTimeHours 过夜判定时长（小时）
     * @param blacklistTypeName 黑名单类型名称
     * @param isPermanent 是否永久拉黑
     * @param blacklistValidDays 临时拉黑有效天数
     * @param nightStartHour 凌晨开始小时
     * @param nightEndHour 凌晨结束小时
     * @return 是否保存成功
     */
    boolean saveCollegeNewCityConfig(String parkCode, String parkName, Integer blacklistTimeHours,
                                     String blacklistTypeName, Boolean isPermanent, Integer blacklistValidDays,
                                     Integer nightStartHour, Integer nightEndHour);

    // ==================== 📊 新增统计分析接口 ====================

    /**
     * 高频违规车辆Top统计
     * @param days 统计天数
     * @param limit 返回数量
     * @return 统计结果
     */
    List<Map<String, Object>> getTopViolators(Integer days, Integer limit);

    /**
     * 违规记录趋势统计
     * @param days 统计天数
     * @return 统计结果
     */
    List<Map<String, Object>> getViolationTrend(Integer days);

    /**
     * 违规类型趋势分析
     * @param days 统计天数
     * @return 统计结果
     */
    List<Map<String, Object>> getViolationTypeTrend(Integer days);

    /**
     * 各位置违规频次统计
     * @param days 统计天数
     * @param location 位置过滤（可选）
     * @return 统计结果
     */
    List<Map<String, Object>> getLocationFrequency(Integer days, String location);

    /**
     * 重复违规车辆预警
     * @param days 统计天数
     * @param threshold 违规次数阈值
     * @return 统计结果
     */
    List<Map<String, Object>> getRepeatViolators(Integer days, Integer threshold);
}
