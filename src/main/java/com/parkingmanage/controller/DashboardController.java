package com.parkingmanage.controller;

import com.parkingmanage.common.Result;
import com.parkingmanage.dto.dashboard.*;
import com.parkingmanage.service.DashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 仪表板控制器
 * </p>
 *
 * @author system
 * @since 2025-09-12
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin
@Api(tags = "仪表板管理")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @ApiOperation("获取实时统计概览")
    @GetMapping("/realtime-overview")
    public Result<RealtimeOverviewDTO> getRealtimeOverview() {
        try {
            RealtimeOverviewDTO overview = dashboardService.getRealtimeOverview();
            return Result.success(overview);
        } catch (Exception e) {
            log.error("获取实时统计概览失败: {}", e.getMessage(), e);
            return Result.error("获取实时统计概览失败");
        }
    }

    @ApiOperation("获取车流量趋势数据")
    @GetMapping("/traffic-trend")
    public Result<TrafficTrendDTO> getTrafficTrend(
            @ApiParam(value = "天数", defaultValue = "7") @RequestParam(defaultValue = "7") Integer days,
            @ApiParam(value = "停车场代码") @RequestParam(required = false) String parkCode) {
        try {
            TrafficTrendDTO trend = dashboardService.getTrafficTrend(days, parkCode);
            return Result.success(trend);
        } catch (Exception e) {
            log.error("获取车流量趋势数据失败: {}", e.getMessage(), e);
            return Result.error("获取车流量趋势数据失败");
        }
    }

    @ApiOperation("获取违规类型分布")
    @GetMapping("/violation-type-distribution")
    public Result<List<ViolationTypeDistributionDTO>> getViolationTypeDistribution(
            @ApiParam(value = "天数", defaultValue = "30") @RequestParam(defaultValue = "30") Integer days) {
        try {
            List<ViolationTypeDistributionDTO> distribution = dashboardService.getViolationTypeDistribution(days);
            return Result.success(distribution);
        } catch (Exception e) {
            log.error("获取违规类型分布失败: {}", e.getMessage(), e);
            return Result.error("获取违规类型分布失败");
        }
    }

    @ApiOperation("获取月票使用率分析")
    @GetMapping("/month-ticket-usage")
    public Result<MonthTicketUsageDTO> getMonthTicketUsage() {
        try {
            MonthTicketUsageDTO usage = dashboardService.getMonthTicketUsage();
            return Result.success(usage);
        } catch (Exception e) {
            log.error("获取月票使用率分析失败: {}", e.getMessage(), e);
            return Result.error("获取月票使用率分析失败");
        }
    }

    // ========== 数据概览模块 ==========
    
    @ApiOperation("各车场预约数量统计")
    @GetMapping("/park-appointment-stats")
    public Result<List<ParkAppointmentStatsDTO>> getParkAppointmentStats(
            @ApiParam(value = "天数", defaultValue = "7") @RequestParam(defaultValue = "7") Integer days) {
        try {
            List<ParkAppointmentStatsDTO> stats = dashboardService.getParkAppointmentStats(days);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取车场预约统计失败: {}", e.getMessage(), e);
            return Result.error("获取车场预约统计失败");
        }
    }

    @ApiOperation("各车场进场数量统计") 
    @GetMapping("/park-entry-stats")
    public Result<List<ParkEntryStatsDTO>> getParkEntryStats(
            @ApiParam(value = "天数", defaultValue = "7") @RequestParam(defaultValue = "7") Integer days) {
        try {
            List<ParkEntryStatsDTO> stats = dashboardService.getParkEntryStats(days);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取车场进场统计失败: {}", e.getMessage(), e);
            return Result.error("获取车场进场统计失败");
        }
    }

    @ApiOperation("业主按小区统计")
    @GetMapping("/owner-community-stats") 
    public Result<List<OwnerCommunityStatsDTO>> getOwnerByCommunityStats() {
        try {
            List<OwnerCommunityStatsDTO> stats = dashboardService.getOwnerByCommunityStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取业主小区统计失败: {}", e.getMessage(), e);
            return Result.error("获取业主小区统计失败");
        }
    }

    @ApiOperation("车辆类型统计")
    @GetMapping("/vehicle-type-stats")
    public Result<List<VehicleTypeStatsDTO>> getVehicleTypeStats() {
        try {
            List<VehicleTypeStatsDTO> stats = dashboardService.getVehicleTypeStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取车辆类型统计失败: {}", e.getMessage(), e);
            return Result.error("获取车辆类型统计失败");
        }
    }

    // ========== 业务分析模块 ==========
    
    @ApiOperation("月票按名称统计")
    @GetMapping("/month-ticket-name-stats")
    public Result<List<MonthTicketNameStatsDTO>> getMonthTicketByNameStats() {
        try {
            List<MonthTicketNameStatsDTO> stats = dashboardService.getMonthTicketByNameStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取月票名称统计失败: {}", e.getMessage(), e);
            return Result.error("获取月票名称统计失败");
        }
    }

    @ApiOperation("月票按车场统计")
    @GetMapping("/month-ticket-park-stats")
    public Result<List<MonthTicketParkStatsDTO>> getMonthTicketByParkStats() {
        try {
            List<MonthTicketParkStatsDTO> stats = dashboardService.getMonthTicketByParkStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取月票车场统计失败: {}", e.getMessage(), e);
            return Result.error("获取月票车场统计失败");
        }
    }

    @ApiOperation("黑名单按违规类型统计")
    @GetMapping("/blacklist-violation-stats") 
    public Result<List<BlacklistViolationStatsDTO>> getBlacklistByViolationStats() {
        try {
            List<BlacklistViolationStatsDTO> stats = dashboardService.getBlacklistByViolationStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取黑名单违规统计失败: {}", e.getMessage(), e);
            return Result.error("获取黑名单违规统计失败");
        }
    }

    @ApiOperation("黑名单按车场统计")
    @GetMapping("/blacklist-park-stats")
    public Result<List<BlacklistParkStatsDTO>> getBlacklistByParkStats() {
        try {
            List<BlacklistParkStatsDTO> stats = dashboardService.getBlacklistByParkStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取黑名单车场统计失败: {}", e.getMessage(), e);
            return Result.error("获取黑名单车场统计失败");
        }
    }

    // ========== 人员管理模块 ==========
    
    @ApiOperation("管家按小区统计")
    @GetMapping("/butler-community-stats")
    public Result<List<ButlerCommunityStatsDTO>> getButlerByCommunityStats() {
        try {
            List<ButlerCommunityStatsDTO> stats = dashboardService.getButlerByCommunityStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取管家小区统计失败: {}", e.getMessage(), e);
            return Result.error("获取管家小区统计失败");
        }
    }

    @ApiOperation("巡逻员按小区统计")
    @GetMapping("/patrol-community-stats")
    public Result<List<PatrolCommunityStatsDTO>> getPatrolByCommunityStats() {
        try {
            List<PatrolCommunityStatsDTO> stats = dashboardService.getPatrolByCommunityStats();
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取巡逻员小区统计失败: {}", e.getMessage(), e);
            return Result.error("获取巡逻员小区统计失败");
        }
    }

    @ApiOperation("预约审批状态统计") 
    @GetMapping("/appointment-approval-stats")
    public Result<List<AppointmentApprovalStatsDTO>> getAppointmentApprovalStats(
            @ApiParam(value = "天数", defaultValue = "30") @RequestParam(defaultValue = "30") Integer days) {
        try {
            List<AppointmentApprovalStatsDTO> stats = dashboardService.getAppointmentApprovalStats(days);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取预约审批统计失败: {}", e.getMessage(), e);
            return Result.error("获取预约审批统计失败");
        }
    }

    @ApiOperation("违规处理统计")
    @GetMapping("/violation-handling-stats")
    public Result<List<ViolationHandlingStatsDTO>> getViolationHandlingStats(
            @ApiParam(value = "天数", defaultValue = "30") @RequestParam(defaultValue = "30") Integer days) {
        try {
            List<ViolationHandlingStatsDTO> stats = dashboardService.getViolationHandlingStats(days);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取违规处理统计失败: {}", e.getMessage(), e);
            return Result.error("获取违规处理统计失败");
        }
    }

    // ========== 放行记录模块 ==========
    
    @ApiOperation("外来车辆放行统计")
    @GetMapping("/vehicle-release-stats")
    public Result<List<VehicleReleaseStatsDTO>> getVehicleReleaseStats(
            @ApiParam(value = "天数", defaultValue = "7") @RequestParam(defaultValue = "7") Integer days) {
        try {
            List<VehicleReleaseStatsDTO> stats = dashboardService.getVehicleReleaseStats(days);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取车辆放行统计失败: {}", e.getMessage(), e);
            return Result.error("获取车辆放行统计失败");
        }
    }

    @ApiOperation("重复放行车辆统计") 
    @GetMapping("/repeat-release-stats")
    public Result<List<RepeatReleaseStatsDTO>> getRepeatReleaseVehicleStats(
            @ApiParam(value = "天数", defaultValue = "30") @RequestParam(defaultValue = "30") Integer days) {
        try {
            List<RepeatReleaseStatsDTO> stats = dashboardService.getRepeatReleaseVehicleStats(days);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取重复放行统计失败: {}", e.getMessage(), e);
            return Result.error("获取重复放行统计失败");
        }
    }

    @ApiOperation("预约转化率统计")
    @GetMapping("/appointment-conversion-stats")
    public Result<List<AppointmentConversionStatsDTO>> getAppointmentConversionStats(
            @ApiParam(value = "天数", defaultValue = "30") @RequestParam(defaultValue = "30") Integer days) {
        try {
            List<AppointmentConversionStatsDTO> stats = dashboardService.getAppointmentConversionStats(days);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取预约转化率统计失败: {}", e.getMessage(), e);
            return Result.error("获取预约转化率统计失败");
        }
    }

    @ApiOperation("小区活跃度排行")
    @GetMapping("/community-activity-ranking")
    public Result<List<CommunityActivityRankingDTO>> getCommunityActivityRanking(
            @ApiParam(value = "天数", defaultValue = "7") @RequestParam(defaultValue = "7") Integer days) {
        try {
            List<CommunityActivityRankingDTO> stats = dashboardService.getCommunityActivityRanking(days);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取小区活跃度排行失败: {}", e.getMessage(), e);
            return Result.error("获取小区活跃度排行失败");
        }
    }
} 