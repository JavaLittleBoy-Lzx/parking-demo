package com.parkingmanage.service;

import com.parkingmanage.dto.dashboard.*;

import java.util.List;

/**
 * Dashboard数据可视化服务接口
 */
public interface DashboardService {

    /**
     * 获取实时统计概览
     * @return 实时统计数据
     */
    RealtimeOverviewDTO getRealtimeOverview();

    /**
     * 获取车流量趋势数据
     * @param days 查询天数
     * @param parkCode 车场编码
     * @return 趋势数据
     */
    TrafficTrendDTO getTrafficTrend(Integer days, String parkCode);

    /**
     * 获取预约通过率趋势
     * @param days 查询天数
     * @return 通过率趋势数据
     */
    AppointmentApprovalTrendDTO getAppointmentApprovalTrend(Integer days);

    /**
     * 获取月票使用率分析
     * @return 月票使用率数据
     */
    MonthTicketUsageDTO getMonthTicketUsage();

    /**
     * 获取违规类型分布
     * @param days 查询天数
     * @return 违规类型分布数据
     */
    List<ViolationTypeDistributionDTO> getViolationTypeDistribution(Integer days);

    /**
     * 获取各车场对比数据
     * @param type 对比类型
     * @param days 查询天数
     * @return 车场对比数据
     */
    List<ParkComparisonDTO> getParkComparison(String type, Integer days);

    /**
     * 获取时段分析数据
     * @param type 分析类型
     * @param days 查询天数
     * @return 时段分析数据
     */
    TimePeriodAnalysisDTO getTimePeriodAnalysis(String type, Integer days);

    /**
     * 获取状态监控数据
     * @param type 监控类型
     * @return 状态监控数据
     */
    StatusMonitoringDTO getStatusMonitoring(String type);

    // ========== 新增的详细分析方法 ==========
    
    /**
     * 获取车场车流量对比数据
     * @param days 查询天数
     * @return 车场车流量对比数据
     */
    ParkTrafficComparisonDTO getParkTrafficComparison(Integer days);
    
    /**
     * 获取车场利用率排行
     * @param days 查询天数
     * @return 车场利用率排行数据
     */
    ParkUtilizationRankingDTO getParkUtilizationRanking(Integer days);
    
    /**
     * 获取24小时车流热力图数据
     * @param days 查询天数
     * @return 车流热力图数据
     */
    TrafficHeatmapDTO getTrafficHeatmap(Integer days);
    
    /**
     * 获取预约时段偏好数据
     * @param days 查询天数
     * @return 预约时段偏好数据
     */
    AppointmentTimePreferenceDTO getAppointmentTimePreference(Integer days);
    
    /**
     * 获取工作日vs周末对比数据
     * @param days 查询天数
     * @return 工作日vs周末对比数据
     */
    WeekdayWeekendComparisonDTO getWeekdayWeekendComparison(Integer days);
    
    /**
     * 获取月票状态分布
     * @return 月票状态分布数据
     */
    List<MonthTicketStatusDistributionDTO> getMonthTicketStatusDistribution();
    
    /**
     * 获取黑名单统计
     * @param days 查询天数
     * @return 黑名单统计数据
     */
    List<BlacklistStatisticsDTO> getBlacklistStatistics(Integer days);
    
    /**
     * 获取预约状态分布
     * @param days 查询天数
     * @return 预约状态分布数据
     */
    List<AppointmentStatusDistributionDTO> getAppointmentStatusDistribution(Integer days);

    // ========== 数据概览模块 ==========
    
    /**
     * 各车场预约数量统计
     * @param days 天数
     * @return 车场预约统计数据
     */
    List<ParkAppointmentStatsDTO> getParkAppointmentStats(Integer days);

    /**
     * 各车场进场数量统计
     * @param days 天数
     * @return 车场进场统计数据
     */
    List<ParkEntryStatsDTO> getParkEntryStats(Integer days);

    /**
     * 业主按小区统计
     * @return 业主小区统计数据
     */
    List<OwnerCommunityStatsDTO> getOwnerByCommunityStats();

    /**
     * 车辆类型统计
     * @return 车辆类型统计数据
     */
    List<VehicleTypeStatsDTO> getVehicleTypeStats();

    // ========== 业务分析模块 ==========
    
    /**
     * 月票按名称统计
     * @return 月票名称统计数据
     */
    List<MonthTicketNameStatsDTO> getMonthTicketByNameStats();

    /**
     * 月票按车场统计
     * @return 月票车场统计数据
     */
    List<MonthTicketParkStatsDTO> getMonthTicketByParkStats();

    /**
     * 黑名单按违规类型统计
     * @return 黑名单违规统计数据
     */
    List<BlacklistViolationStatsDTO> getBlacklistByViolationStats();

    /**
     * 黑名单按车场统计
     * @return 黑名单车场统计数据
     */
    List<BlacklistParkStatsDTO> getBlacklistByParkStats();

    // ========== 人员管理模块 ==========
    
    /**
     * 管家按小区统计
     * @return 管家小区统计数据
     */
    List<ButlerCommunityStatsDTO> getButlerByCommunityStats();

    /**
     * 巡逻员按小区统计
     * @return 巡逻员小区统计数据
     */
    List<PatrolCommunityStatsDTO> getPatrolByCommunityStats();

    /**
     * 预约审批状态统计
     * @param days 天数
     * @return 预约审批统计数据
     */
    List<AppointmentApprovalStatsDTO> getAppointmentApprovalStats(Integer days);

    /**
     * 违规处理统计
     * @param days 天数
     * @return 违规处理统计数据
     */
    List<ViolationHandlingStatsDTO> getViolationHandlingStats(Integer days);

    // ========== 放行记录模块 ==========
    
    /**
     * 外来车辆放行统计
     * @param days 天数
     * @return 车辆放行统计数据
     */
    List<VehicleReleaseStatsDTO> getVehicleReleaseStats(Integer days);

    /**
     * 重复放行车辆统计
     * @param days 天数
     * @return 重复放行统计数据
     */
    List<RepeatReleaseStatsDTO> getRepeatReleaseVehicleStats(Integer days);

    /**
     * 预约转化率统计
     * @param days 天数
     * @return 预约转化率统计数据
     */
    List<AppointmentConversionStatsDTO> getAppointmentConversionStats(Integer days);

    /**
     * 小区活跃度排行
     * @param days 天数
     * @return 小区活跃度排行数据
     */
    List<CommunityActivityRankingDTO> getCommunityActivityRanking(Integer days);
} 