package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.ViolationConfig;
import com.parkingmanage.mapper.ViolationConfigMapper;
import com.parkingmanage.service.ViolationConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 违规配置 服务实现类
 *
 * @author System
 */
@Service
@Slf4j
public class ViolationConfigServiceImpl extends ServiceImpl<ViolationConfigMapper, ViolationConfig> implements ViolationConfigService {

    @Override
    public boolean saveOrUpdateConfig(String parkName, String parkCode, String configType,
                                     Integer maxViolationCount, String blacklistType,
                                     Boolean isPermanent, Integer blacklistValidDays,
                                     String operatorId,Integer reminderIntervalMinutes) {
        log.info("💾 [保存违规配置] parkName={}, configType={}, maxCount={}, blacklistType={}, isPermanent={}, validDays={}, reminderIntervalMinutes= {}",
                parkName, configType, maxViolationCount, blacklistType, isPermanent, blacklistValidDays,reminderIntervalMinutes);
        try {
            // 查询是否已存在配置
            QueryWrapper<ViolationConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("park_name", parkName);
            queryWrapper.eq("config_type", configType);
            ViolationConfig existingConfig = baseMapper.selectOne(queryWrapper);
            
            if (existingConfig != null) {
                // 更新现有配置
                existingConfig.setParkCode(parkCode);
                existingConfig.setMaxViolationCount(maxViolationCount);
                existingConfig.setBlacklistType(blacklistType);
                existingConfig.setIsPermanent(isPermanent);
                existingConfig.setBlacklistValidDays(blacklistValidDays);  // 保存有效天数
                existingConfig.setUpdatedBy(operatorId);
                existingConfig.setUpdatedAt(LocalDateTime.now());
                existingConfig.setReminderIntervalMinutes(reminderIntervalMinutes);
                existingConfig.setIsActive(true);
                
                // 构建配置说明
                String description = buildDescription(maxViolationCount, blacklistType, isPermanent, 
                                                     blacklistValidDays,reminderIntervalMinutes);
                existingConfig.setDescription(description);
                
                boolean result = baseMapper.updateById(existingConfig) > 0;
                log.info("✅ [配置更新{}] parkName={}, configType={}, validDays={}", result ? "成功" : "失败", parkName, configType, blacklistValidDays);
                return result;
            } else {
                // 创建新配置
                ViolationConfig config = new ViolationConfig();
                config.setParkName(parkName);
                config.setParkCode(parkCode);
                config.setConfigType(configType);
                config.setMaxViolationCount(maxViolationCount);
                config.setBlacklistType(blacklistType);
                config.setIsPermanent(isPermanent);
                config.setBlacklistValidDays(blacklistValidDays);  // 保存有效天数
                config.setIsActive(true);
                config.setCreatedBy(operatorId);
                config.setCreatedAt(LocalDateTime.now());
                config.setUpdatedAt(LocalDateTime.now());
                
                // 构建配置说明
                String description = buildDescription(maxViolationCount, blacklistType, isPermanent, 
                                                     blacklistValidDays,blacklistValidDays);
                config.setDescription(description);
                
                boolean result = baseMapper.insert(config) > 0;
                log.info("✅ [配置创建{}] parkName={}, configType={}, validDays={}", result ? "成功" : "失败", parkName, configType, blacklistValidDays);
                return result;
            }
            
        } catch (Exception e) {
            log.error("❌ [配置保存异常] parkName={}, configType={}, error={}", parkName, configType, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public ViolationConfig getByParkNameAndType(String parkName, String configType) {
        log.info("🔍 [查询违规配置] parkName={}, configType={}", parkName, configType);
        
        try {
            QueryWrapper<ViolationConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("park_name", parkName);
            queryWrapper.eq("config_type", configType);
            queryWrapper.eq("is_active", true);
            
            ViolationConfig config = baseMapper.selectOne(queryWrapper);
            log.info("📋 [配置查询结果] parkName={}, configType={}, found={}", parkName, configType, config != null);
            
            return config;
        } catch (Exception e) {
            log.error("❌ [配置查询异常] parkName={}, configType={}, error={}", parkName, configType, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public ViolationConfig getByParkCodeAndType(String parkCode, String configType) {
        log.info("🔍 [查询违规配置] parkCode={}, configType={}", parkCode, configType);
        
        try {
            QueryWrapper<ViolationConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("park_code", parkCode);
            queryWrapper.eq("config_type", configType);
            queryWrapper.eq("is_active", true);
            
            ViolationConfig config = baseMapper.selectOne(queryWrapper);
            log.info("📋 [配置查询结果] parkCode={}, configType={}, found={}", parkCode, configType, config != null);
            
            return config;
        } catch (Exception e) {
            log.error("❌ [配置查询异常] parkCode={}, configType={}, error={}", parkCode, configType, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean deleteByParkNameAndType(String parkName, String configType) {
        log.info("🗑️ [删除违规配置] parkName={}, configType={}", parkName, configType);
        
        try {
            QueryWrapper<ViolationConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("park_name", parkName);
            queryWrapper.eq("config_type", configType);
            
            boolean result = baseMapper.delete(queryWrapper) > 0;
            log.info("✅ [配置删除{}] parkName={}, configType={}", result ? "成功" : "失败", parkName, configType);
            
            return result;
        } catch (Exception e) {
            log.error("❌ [配置删除异常] parkName={}, configType={}, error={}", parkName, configType, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建配置说明
     */
    private String buildDescription(Integer maxViolationCount, String blacklistType, 
                                   Boolean isPermanent, Integer validDays, Integer reminderIntervalMinutes) {
        StringBuilder desc = new StringBuilder();
        desc.append("违规").append(maxViolationCount).append("次自动拉黑，");
        desc.append("黑名单类型：").append(blacklistType).append("，");
        
        if (Boolean.TRUE.equals(isPermanent)) {
            desc.append("永久拉黑");
        } else {
            desc.append("临时拉黑");
            if (validDays != null) {
                desc.append("（有效期").append(validDays).append("天，从最后一次违规时间开始计算）");
                // 添加拉黑时间间隔
                desc.append("，提醒间隔").append(reminderIntervalMinutes).append("分钟");
            }
        }
        
        return desc.toString();
    }

    @Override
    public int getReminderIntervalMinutes(String parkName, int defaultMinutes) {
        final String configType = "NEBU_AUTO_BLACKLIST";
        try {
            QueryWrapper<ViolationConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("park_name", parkName);
            queryWrapper.eq("config_type", configType);
            queryWrapper.eq("is_active", 1);

            ViolationConfig config = baseMapper.selectOne(queryWrapper);
            Integer minutes = config != null ? config.getReminderIntervalMinutes() : null;
            System.out.println("minutes = " + minutes);
            return minutes != null && minutes > 0 ? minutes : defaultMinutes;
        } catch (Exception e) {
            log.error("❌ [获取提醒间隔异常] parkName={}, error={}", parkName, e.getMessage(), e);
            return defaultMinutes;
        }
    }

    @Override
    public boolean updateReminderIntervalMinutes(String parkName, int minutes, String operatorId) {
        final String configType = "VIOLATION_REMINDER";
        log.info("💾 [保存提醒间隔] parkName={}, minutes={}", parkName, minutes);
        try {
            QueryWrapper<ViolationConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("park_name", parkName);
            queryWrapper.eq("config_type", configType);

            ViolationConfig existing = baseMapper.selectOne(queryWrapper);
            if (existing != null) {
                existing.setReminderIntervalMinutes(minutes);
                existing.setIsActive(true);
                existing.setUpdatedBy(operatorId);
                existing.setUpdatedAt(LocalDateTime.now());
                existing.setDescription("违规提醒最小发送间隔为" + minutes + "分钟");
                return baseMapper.updateById(existing) > 0;
            } else {
                ViolationConfig config = new ViolationConfig();
                config.setParkName(parkName);
                config.setParkCode(null);
                config.setConfigType(configType);
                config.setReminderIntervalMinutes(minutes);
                config.setIsActive(true);
                config.setCreatedBy(operatorId);
                config.setCreatedAt(LocalDateTime.now());
                config.setUpdatedAt(LocalDateTime.now());
                config.setDescription("违规提醒最小发送间隔为" + minutes + "分钟");
                return baseMapper.insert(config) > 0;
            }
        } catch (Exception e) {
            log.error("❌ [保存提醒间隔异常] parkName={}, error={}", parkName, e.getMessage(), e);
            return false;
        }
    }
}

