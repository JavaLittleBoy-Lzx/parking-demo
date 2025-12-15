package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.MonthlyTicketTimeoutConfig;
import com.parkingmanage.mapper.MonthlyTicketTimeoutConfigMapper;
import com.parkingmanage.service.MonthlyTicketTimeoutConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 月票车超时配置 服务实现类
 *
 * @author System
 */
@Service
@Slf4j
public class MonthlyTicketTimeoutConfigServiceImpl extends ServiceImpl<MonthlyTicketTimeoutConfigMapper, MonthlyTicketTimeoutConfig> implements MonthlyTicketTimeoutConfigService {

    @Override
    public boolean saveOrUpdateConfig(String parkCode, String parkName, Integer timeoutMinutes, Integer maxViolationCount, String operatorId) {
        log.info("💾 [保存月票车超时配置] parkCode={}, timeoutMinutes={}, maxViolationCount={}", parkCode, timeoutMinutes, maxViolationCount);
        
        try {
            // 查询是否已存在配置
            QueryWrapper<MonthlyTicketTimeoutConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("park_code", parkCode);
            MonthlyTicketTimeoutConfig existingConfig = baseMapper.selectOne(queryWrapper);
            
            if (existingConfig != null) {
                // 更新现有配置
                existingConfig.setParkName(parkName);
                existingConfig.setTimeoutMinutes(timeoutMinutes);
                existingConfig.setMaxViolationCount(maxViolationCount);
                existingConfig.setDescription("月票车超时配置: timeout=" + timeoutMinutes + "分钟,maxCount=" + maxViolationCount + "次");
                existingConfig.setUpdatedAt(LocalDateTime.now());
                existingConfig.setIsActive(true);
                
                boolean result = baseMapper.updateById(existingConfig) > 0;
                log.info("✅ [配置更新{}] parkCode={}", result ? "成功" : "失败", parkCode);
                return result;
            } else {
                // 创建新配置
                MonthlyTicketTimeoutConfig config = new MonthlyTicketTimeoutConfig();
                config.setParkCode(parkCode);
                config.setParkName(parkName);
                config.setTimeoutMinutes(timeoutMinutes);
                config.setMaxViolationCount(maxViolationCount);
                config.setDescription("月票车超时配置: timeout=" + timeoutMinutes + "分钟,maxCount=" + maxViolationCount + "次");
                config.setIsActive(true);
                config.setCreatedBy(operatorId);
                config.setCreatedAt(LocalDateTime.now());
                config.setUpdatedAt(LocalDateTime.now());
                
                boolean result = baseMapper.insert(config) > 0;
                log.info("✅ [配置创建{}] parkCode={}", result ? "成功" : "失败", parkCode);
                return result;
            }
            
        } catch (Exception e) {
            log.error("❌ [配置保存异常] parkCode={}, error={}", parkCode, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public MonthlyTicketTimeoutConfig getByParkCode(String parkCode) {
        log.info("🔍 [查询月票车超时配置] parkCode={}", parkCode);
        
        try {
            QueryWrapper<MonthlyTicketTimeoutConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("park_code", parkCode);
            queryWrapper.eq("is_active", true);
            
            MonthlyTicketTimeoutConfig config = baseMapper.selectOne(queryWrapper);
            log.info("📋 [配置查询结果] parkCode={}, found={}", parkCode, config != null);
            
            return config;
        } catch (Exception e) {
            log.error("❌ [配置查询异常] parkCode={}, error={}", parkCode, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public boolean deleteByParkCode(String parkCode) {
        log.info("🗑️ [删除月票车超时配置] parkCode={}", parkCode);
        
        try {
            QueryWrapper<MonthlyTicketTimeoutConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("park_code", parkCode);
            
            boolean result = baseMapper.delete(queryWrapper) > 0;
            log.info("✅ [配置删除{}] parkCode={}", result ? "成功" : "失败", parkCode);
            
            return result;
        } catch (Exception e) {
            log.error("❌ [配置删除异常] parkCode={}, error={}", parkCode, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean saveOrUpdateFullConfig(String parkCode, String parkName, Integer timeoutMinutes, 
                                         Integer maxViolationCount, String nightStartTime, String nightEndTime,
                                         Integer nightTimeHours, Boolean enableOvernightCheck, String operatorId) {
        log.info("💾 [保存完整配置] parkCode={}, timeout={}分钟, maxCount={}, night={}:{}-{} {}小时, enabled={}, operator={}", 
                parkCode, timeoutMinutes, maxViolationCount, nightStartTime, nightEndTime, nightTimeHours, 
                enableOvernightCheck, operatorId);
        
        try {
            // 查询是否已存在配置
            QueryWrapper<MonthlyTicketTimeoutConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("park_code", parkCode);
            
            MonthlyTicketTimeoutConfig existingConfig = baseMapper.selectOne(queryWrapper);

            if (existingConfig != null) {
                // 更新现有配置
                existingConfig.setParkName(parkName);
                existingConfig.setTimeoutMinutes(timeoutMinutes);
                existingConfig.setMaxViolationCount(maxViolationCount);
                existingConfig.setNightStartTime(nightStartTime);
                existingConfig.setNightEndTime(nightEndTime);
                existingConfig.setNightTimeHours(nightTimeHours);
                existingConfig.setEnableOvernightCheck(enableOvernightCheck ? 1 : 0);
                existingConfig.setDescription(buildConfigDescription(timeoutMinutes, maxViolationCount, 
                                                                   nightStartTime, nightEndTime, nightTimeHours));
                existingConfig.setUpdatedAt(LocalDateTime.now());
                
                boolean result = baseMapper.updateById(existingConfig) > 0;
                log.info("✅ [完整配置更新{}] parkCode={}", result ? "成功" : "失败", parkCode);
                return result;
            } else {
                // 创建新配置
                MonthlyTicketTimeoutConfig config = new MonthlyTicketTimeoutConfig();
                config.setParkCode(parkCode);
                config.setParkName(parkName);
                config.setTimeoutMinutes(timeoutMinutes);
                config.setMaxViolationCount(maxViolationCount);
                config.setNightStartTime(nightStartTime);
                config.setNightEndTime(nightEndTime);
                config.setNightTimeHours(nightTimeHours);
                config.setEnableOvernightCheck(enableOvernightCheck ? 1 : 0);
                config.setDescription(buildConfigDescription(timeoutMinutes, maxViolationCount, 
                                                           nightStartTime, nightEndTime, nightTimeHours));
                config.setIsActive(true);
                config.setCreatedBy(operatorId);
                config.setCreatedAt(LocalDateTime.now());
                config.setUpdatedAt(LocalDateTime.now());
                
                boolean result = baseMapper.insert(config) > 0;
                log.info("✅ [完整配置创建{}] parkCode={}", result ? "成功" : "失败", parkCode);
                return result;
            }
            
        } catch (Exception e) {
            log.error("❌ [完整配置保存异常] parkCode={}, error={}", parkCode, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean saveOrUpdateOvernightConfig(String parkCode, String parkName, Integer timeoutMinutes, 
                                              Integer maxViolationCount, Integer overnightTimeHours, 
                                              Boolean enableOvernightCheck, String operatorId) {
        log.info("💾 [保存月票车过夜配置] parkCode={}, timeoutMinutes={}, maxViolationCount={}, overnightHours={}, enabled={}", 
                parkCode, timeoutMinutes, maxViolationCount, overnightTimeHours, enableOvernightCheck);
        
        try {
            // 查询是否已存在配置
            QueryWrapper<MonthlyTicketTimeoutConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("park_code", parkCode);
            MonthlyTicketTimeoutConfig existingConfig = baseMapper.selectOne(queryWrapper);
            
            if (existingConfig != null) {
                // 更新现有配置
                existingConfig.setParkName(parkName);
                existingConfig.setTimeoutMinutes(timeoutMinutes);
                existingConfig.setMaxViolationCount(maxViolationCount);
                existingConfig.setNightTimeHours(overnightTimeHours);
                existingConfig.setEnableOvernightCheck(enableOvernightCheck ? 1 : 0);
                existingConfig.setDescription(buildOvernightConfigDescription(timeoutMinutes, maxViolationCount, overnightTimeHours));
                existingConfig.setUpdatedAt(LocalDateTime.now());
                existingConfig.setIsActive(true);
                
                boolean result = baseMapper.updateById(existingConfig) > 0;
                log.info("✅ [过夜配置更新{}] parkCode={}", result ? "成功" : "失败", parkCode);
                return result;
            } else {
                // 创建新配置
                MonthlyTicketTimeoutConfig config = new MonthlyTicketTimeoutConfig();
                config.setParkCode(parkCode);
                config.setParkName(parkName);
                config.setTimeoutMinutes(timeoutMinutes);
                config.setMaxViolationCount(maxViolationCount);
                config.setNightTimeHours(overnightTimeHours);
                config.setEnableOvernightCheck(enableOvernightCheck ? 1 : 0);
                config.setDescription(buildOvernightConfigDescription(timeoutMinutes, maxViolationCount, overnightTimeHours));
                config.setIsActive(true);
                config.setCreatedBy(operatorId);
                config.setCreatedAt(LocalDateTime.now());
                config.setUpdatedAt(LocalDateTime.now());
                
                boolean result = baseMapper.insert(config) > 0;
                log.info("✅ [过夜配置创建{}] parkCode={}", result ? "成功" : "失败", parkCode);
                return result;
            }
            
        } catch (Exception e) {
            log.error("❌ [过夜配置保存异常] parkCode={}, error={}", parkCode, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 构建配置描述
     */
    private String buildConfigDescription(Integer timeoutMinutes, Integer maxViolationCount,
                                        String nightStartTime, String nightEndTime, Integer nightTimeHours) {
        return String.format("月票车配置: 超时%d分钟,累计%d次拉黑; 过夜(%s-%s)超过%d小时直接拉黑",
                           timeoutMinutes, maxViolationCount, nightStartTime, nightEndTime, nightTimeHours);
    }
    
    /**
     * 🆕 构建新版过夜配置描述
     */
    private String buildOvernightConfigDescription(Integer timeoutMinutes, Integer maxViolationCount, Integer overnightTimeHours) {
        return String.format("月票车配置: 超时%d分钟,累计%d次拉黑; 过夜超过%d小时直接拉黑",
                           timeoutMinutes, maxViolationCount, overnightTimeHours);
    }
} 