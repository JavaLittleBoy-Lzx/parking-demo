package com.parkingmanage.service;

import java.time.LocalDateTime;

/**
 * 过夜停车判定服务
 * 
 * @author parkingmanage
 * @since 2025-09-19
 */
public interface OvernightParkingService {
    
    /**
     * 判断是否为过夜停车
     * 
     * @param enterTime 进场时间
     * @param leaveTime 离场时间（为null时表示当前还在场内）
     * @param nightStartTime 夜间开始时间（如：22:00）
     * @param nightEndTime 夜间结束时间（如：06:00）
     * @param nightTimeHours 夜间时段内停车超过X小时算违规
     * @return true-过夜违规，false-正常停车
     */
    boolean isOvernightViolation(LocalDateTime enterTime, LocalDateTime leaveTime, 
                               String nightStartTime, String nightEndTime, int nightTimeHours);
    
    /**
     * 计算在夜间时段内的停车时长（小时）
     * 
     * @param enterTime 进场时间
     * @param leaveTime 离场时间（为null时表示当前还在场内）
     * @param nightStartTime 夜间开始时间（如：22:00）
     * @param nightEndTime 夜间结束时间（如：06:00）
     * @return 夜间时段内的停车时长（小时）
     */
    double calculateNightParkingHours(LocalDateTime enterTime, LocalDateTime leaveTime,
                                    String nightStartTime, String nightEndTime);
    
    /**
     * 获取过夜停车详细信息
     * 
     * @param enterTime 进场时间
     * @param leaveTime 离场时间
     * @param nightStartTime 夜间开始时间
     * @param nightEndTime 夜间结束时间
     * @param nightTimeHours 夜间时段限制小时数
     * @return 过夜停车分析结果
     */
    OvernightParkingAnalysis analyzeOvernightParking(LocalDateTime enterTime, LocalDateTime leaveTime,
                                                   String nightStartTime, String nightEndTime, int nightTimeHours);
    
    /**
     * 过夜停车分析结果
     */
    class OvernightParkingAnalysis {
        private boolean isViolation;              // 是否违规
        private double totalParkingHours;         // 总停车时长（小时）
        private double nightParkingHours;         // 夜间时段停车时长（小时）
        private String violationReason;           // 违规原因
        private String description;               // 详细描述
        
        // 构造函数
        public OvernightParkingAnalysis(boolean isViolation, double totalParkingHours, 
                                      double nightParkingHours, String violationReason, String description) {
            this.isViolation = isViolation;
            this.totalParkingHours = totalParkingHours;
            this.nightParkingHours = nightParkingHours;
            this.violationReason = violationReason;
            this.description = description;
        }
        
        // Getters
        public boolean isViolation() { return isViolation; }
        public double getTotalParkingHours() { return totalParkingHours; }
        public double getNightParkingHours() { return nightParkingHours; }
        public String getViolationReason() { return violationReason; }
        public String getDescription() { return description; }
        
        // Setters
        public void setViolation(boolean violation) { isViolation = violation; }
        public void setTotalParkingHours(double totalParkingHours) { this.totalParkingHours = totalParkingHours; }
        public void setNightParkingHours(double nightParkingHours) { this.nightParkingHours = nightParkingHours; }
        public void setViolationReason(String violationReason) { this.violationReason = violationReason; }
        public void setDescription(String description) { this.description = description; }
    }
} 