package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.SystemConfig;
import com.parkingmanage.mapper.SystemConfigMapper;
import com.parkingmanage.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 */
@Slf4j
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    @Override
    public String getConfigValue(String configKey) {
        SystemConfig config = this.getOne(
            new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey)
                .eq(SystemConfig::getStatus, 1)
        );
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        String value = getConfigValue(configKey);
        return value != null ? value : defaultValue;
    }

    @Override
    public boolean setConfigValue(String configKey, String configValue) {
        SystemConfig config = this.getOne(
            new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey)
        );
        
        if (config != null) {
            config.setConfigValue(configValue);
            return this.updateById(config);
        } else {
            // 如果配置不存在，创建新配置
            config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigName(configKey);
            config.setConfigType("system");
            config.setEditable(1);
            config.setStatus(1);
            return this.save(config);
        }
    }

    @Override
    public List<SystemConfig> getConfigsByType(String configType) {
        return this.list(
            new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigType, configType)
                .eq(SystemConfig::getStatus, 1)
                .orderByAsc(SystemConfig::getSortOrder)
        );
    }

    @Override
    public List<SystemConfig> getAllEnabledConfigs() {
        return this.list(
            new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getStatus, 1)
                .orderByAsc(SystemConfig::getConfigType)
                .orderByAsc(SystemConfig::getSortOrder)
        );
    }

    @Override
    public boolean updateConfigs(Map<String, String> configs) {
        try {
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                setConfigValue(entry.getKey(), entry.getValue());
            }
            return true;
        } catch (Exception e) {
            log.error("批量更新配置失败", e);
            return false;
        }
    }

    @Override
    public Map<String, String> getConfigMap() {
        List<SystemConfig> configs = getAllEnabledConfigs();
        return configs.stream()
            .collect(Collectors.toMap(
                SystemConfig::getConfigKey,
                SystemConfig::getConfigValue,
                (existing, replacement) -> replacement
            ));
    }

    @Override
    public Map<String, String> getConfigMapByType(String configType) {
        List<SystemConfig> configs = getConfigsByType(configType);
        return configs.stream()
            .collect(Collectors.toMap(
                SystemConfig::getConfigKey,
                SystemConfig::getConfigValue,
                (existing, replacement) -> replacement
            ));
    }
}
