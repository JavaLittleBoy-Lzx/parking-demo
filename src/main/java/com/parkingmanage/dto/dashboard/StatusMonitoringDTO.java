package com.parkingmanage.dto.dashboard;

import lombok.Data;

import java.util.List;

/**
 * 状态监控数据传输对象
 */
@Data
public class StatusMonitoringDTO {
    /**
     * 在线设备数量
     */
    private Integer onlineDeviceCount;
    
    /**
     * 离线设备数量
     */
    private Integer offlineDeviceCount;
    
    /**
     * 设备总数
     */
    private Integer totalDeviceCount;
    
    /**
     * 在线率
     */
    private Double onlineRate;
    
    /**
     * 设备状态详情
     */
    private List<DeviceStatus> deviceStatuses;
    
    @Data
    public static class DeviceStatus {
        /**
         * 设备名称
         */
        private String deviceName;
        
        /**
         * 设备编号
         */
        private String deviceCode;
        
        /**
         * 设备类型
         */
        private String deviceType;
        
        /**
         * 设备状态
         */
        private String status;
        
        /**
         * 最后在线时间
         */
        private String lastOnlineTime;
    }
} 