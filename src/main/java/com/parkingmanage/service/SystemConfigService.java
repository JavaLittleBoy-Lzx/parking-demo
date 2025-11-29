package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.SystemConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService extends IService<SystemConfig> {

    /**
     * 根据配置键获取配置值
     */
    String getConfigValue(String configKey);

    /**
     * 根据配置键获取配置值，如果不存在返回默认值
     */
    String getConfigValue(String configKey, String defaultValue);

    /**
     * 设置配置值
     */
    boolean setConfigValue(String configKey, String configValue);

    /**
     * 根据配置类型获取配置列表
     */
    List<SystemConfig> getConfigsByType(String configType);

    /**
     * 获取所有启用的配置
     */
    List<SystemConfig> getAllEnabledConfigs();

    /**
     * 批量更新配置
     */
    boolean updateConfigs(Map<String, String> configs);

    /**
     * 获取配置Map
     */
    Map<String, String> getConfigMap();

    /**
     * 获取配置Map（按类型）
     */
    Map<String, String> getConfigMapByType(String configType);
}
