package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.entity.Violations;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 违规记录表 Mapper 接口
 * </p>
 *
 * @author MLH
 * @since 2025-01-31
 */
public interface ViolationsMapper extends BaseMapper<Violations> {

    /**
     * 分页查询违规记录（支持小区过滤）
     */
    IPage<Map<String, Object>> selectViolationsWithOwnerInfo(
            Page<Map<String, Object>> page,
            @Param("plateNumber") String plateNumber,
            @Param("status") String status,
            @Param("violationType") String violationType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("createdByFilter") String createdByFilter,
            @Param("communityFilter") String communityFilter
    );

    /**
     * 🎓 分页查询违规记录（直接查询模式，不关联其他表）
     * 专为东北林业大学等使用violations表直接存储业主信息的场景设计
     */
    IPage<Map<String, Object>> selectViolationsDirectQuery(
            Page<Map<String, Object>> page,
            @Param("plateNumber") String plateNumber,
            @Param("status") String status,
            @Param("violationType") String violationType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("createdByFilter") String createdByFilter,
            @Param("communityFilter") String communityFilter
    );

    /**
     * 获取高风险车辆列表
     */
    List<Map<String, Object>> selectHighRiskVehicles(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("limit") Integer limit
    );

    /**
     * 🎓 获取高风险车辆列表（直接查询模式，支持创建者和小区过滤）
     */
    List<Map<String, Object>> selectHighRiskVehiclesDirectQuery(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("limit") Integer limit,
            @Param("createdByFilter") String createdByFilter,
            @Param("communityFilter") String communityFilter
    );

    /**
     * 获取违规统计数据
     */
    List<Map<String, Object>> selectViolationStatistics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("plateNumber") String plateNumber
    );

    /**
     * 🎓 获取违规统计数据（支持创建者和小区过滤）
     */
    List<Map<String, Object>> selectViolationStatisticsWithFilter(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("plateNumber") String plateNumber,
            @Param("createdByFilter") String createdByFilter,
            @Param("communityFilter") String communityFilter
    );

    /**
     * 根据车牌号查询车主ID
     */
    Integer selectOwnerIdByPlateNumber(@Param("plateNumber") String plateNumber);

    /**
     * 更新车主信用分
     */
    int updateOwnerCreditScore(@Param("ownerId") Integer ownerId, @Param("deduction") Integer deduction);

    /**
     * 按日期统计违规数量
     */
    List<Map<String, Object>> selectDailyViolationStats(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("plateNumber") String plateNumber
    );

    /**
     * 🎓 按日期统计违规数量（支持创建者和小区过滤）
     */
    List<Map<String, Object>> selectDailyViolationStatsWithFilter(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("plateNumber") String plateNumber,
            @Param("createdByFilter") String createdByFilter,
            @Param("communityFilter") String communityFilter
    );

    /**
     * 按违规类型统计
     */
    List<Map<String, Object>> selectViolationTypeStats(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 🎓 按违规类型统计（支持创建者和小区过滤）
     */
    List<Map<String, Object>> selectViolationTypeStatsWithFilter(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("createdByFilter") String createdByFilter,
            @Param("communityFilter") String communityFilter
    );

    /**
     * 根据车牌号查询预约记录（用于违规录入）
     */
    List<Map<String, Object>> selectAppointmentRecordsByPlate(@Param("plateNumber") String plateNumber);

    /**
     * 🆕 根据预约记录ID查询业主信息
     */
    Map<String, Object> selectOwnerByAppointmentId(@Param("appointmentId") Integer appointmentId);

    /**
     * 🆕 根据车牌号、停车场编码和进场时间查询违规记录
     * @param plateNumber 车牌号
     * @param parkCode 停车场编码
     * @param enterTime 进场时间（数据库格式）
     * @return 违规记录
     */
    Violations selectByPlateAndParkCodeAndEnterTime(
            @Param("plateNumber") String plateNumber,
            @Param("parkCode") String parkCode,
            @Param("enterTime") LocalDateTime enterTime
    );

    /**
     * 🆕 更新违规记录的离场时间
     * @param id 记录ID
     * @param leaveTime 离场时间
     * @return 更新行数
     */
    int updateLeaveTimeById(@Param("id") Long id, @Param("leaveTime") LocalDateTime leaveTime);
    
    /**
     * 🆕 根据车牌号查询月票车主信息
     * @param plateNumber 车牌号
     * @return 月票车主信息
     */
    Map<String, Object> selectOwnerByPlateNumber(@Param("plateNumber") String plateNumber);

    // ==================== 🆕 违规记录处理功能相关方法 ====================

    /**
     * 统计指定车牌的未处理违规次数
     * @param plateNumber 车牌号
     * @return 未处理违规次数
     */
    int countUnprocessedByPlate(@Param("plateNumber") String plateNumber);

    /**
     * 批量更新指定车牌的违规记录处理状态
     * @param plateNumber 车牌号
     * @param processStatus 处理状态
     * @param processType 处理方式
     * @param processedAt 处理时间
     * @param processedBy 处理人
     * @param processRemark 处理备注
     * @return 更新的记录数
     */
    int batchUpdateProcessStatusByPlate(
            @Param("plateNumber") String plateNumber,
            @Param("processStatus") String processStatus,
            @Param("processType") String processType,
            @Param("processedAt") LocalDateTime processedAt,
            @Param("processedBy") String processedBy,
            @Param("processRemark") String processRemark
    );

    /**
     * 分页查询违规记录（直接查询模式，支持处理状态筛选）
     * @param page 分页参数
     * @param plateNumber 车牌号
     * @param status 状态
     * @param violationType 违规类型
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param createdByFilter 创建者过滤
     * @param communityFilter 小区过滤
     * @param processStatus 处理状态筛选 (pending/processed)
     * @param processType 处理方式筛选 (auto_blacklist/manual)
     * @param onlyUnprocessed 是否仅查询未处理
     * @return 违规记录分页数据
     */
    IPage<Map<String, Object>> selectViolationsDirectQueryWithProcess(
            Page<Map<String, Object>> page,
            @Param("plateNumber") String plateNumber,
            @Param("status") String status,
            @Param("violationType") String violationType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("createdByFilter") String createdByFilter,
            @Param("communityFilter") String communityFilter,
            @Param("processStatus") String processStatus,
            @Param("processType") String processType,
            @Param("onlyUnprocessed") Boolean onlyUnprocessed
    );

    // ==================== 📊 新增统计图表查询方法 ====================

    /**
     * 1. 高频违规车辆Top N统计
     * @param days 统计天数
     * @param limit 返回数量
     * @return 高频违规车辆列表
     */
    List<Map<String, Object>> selectTopViolators(
            @Param("days") Integer days,
            @Param("limit") Integer limit
    );

    /**
     * 2. 违规记录趋势统计（按日期分组）
     * @param days 统计天数
     * @return 每日违规数量列表
     */
    List<Map<String, Object>> selectViolationTrend(@Param("days") Integer days);

    /**
     * 3. 违规类型趋势分析（按日期和类型分组）
     * @param days 统计天数
     * @return 每日各类型违规数量列表
     */
    List<Map<String, Object>> selectViolationTypeTrend(@Param("days") Integer days);

    /**
     * 4. 各位置违规频次统计
     * @param days 统计天数
     * @param location 位置过滤（可选）
     * @return 各位置违规数量列表
     */
    List<Map<String, Object>> selectLocationFrequency(
            @Param("days") Integer days,
            @Param("location") String location
    );

    /**
     * 5. 重复违规车辆预警分析
     * @param days 统计天数
     * @param threshold 违规次数阈值
     * @return 重复违规车辆列表
     */
    List<Map<String, Object>> selectRepeatViolators(
            @Param("days") Integer days,
            @Param("threshold") Integer threshold
    );

    /**
     * 🆕 非东林车场：分页查询违规记录（关联 vehicle_reservation 表）
     * 用于万象上东等后台预约车场
     */
    IPage<Map<String, Object>> selectViolationsWithReservation(
            Page<Map<String, Object>> page,
            @Param("plateNumber") String plateNumber,
            @Param("status") String status,
            @Param("violationType") String violationType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("createdByFilter") String createdByFilter,
            @Param("communityFilter") String communityFilter
    );
}
