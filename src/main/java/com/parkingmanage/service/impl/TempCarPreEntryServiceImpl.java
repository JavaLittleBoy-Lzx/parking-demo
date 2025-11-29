package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.TempCarPreEntry;
import com.parkingmanage.mapper.TempCarPreEntryMapper;
import com.parkingmanage.service.TempCarPreEntryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 临时车预进场数据服务实现类
 * 
 * @author lzx
 */
@Service
public class TempCarPreEntryServiceImpl extends ServiceImpl<TempCarPreEntryMapper, TempCarPreEntry> 
        implements TempCarPreEntryService {
    
    private static final Logger logger = LoggerFactory.getLogger(TempCarPreEntryServiceImpl.class);
    
    @Override
    public boolean saveTempCarPreEntry(String plateNumber, String parkCode, String parkName, 
                                      String enterChannelCode, Integer enterChannelId, 
                                      Integer enterVipType, String preEnterTime) {
        try {
            logger.info("🚗 [保存临时车预进场数据] plateNumber={}, parkCode={}, enterChannelCode={}", 
                    plateNumber, parkCode, enterChannelCode);
            // 先查询是否已存在该车牌号的记录（不限制是否使用）
            TempCarPreEntry existingEntry = baseMapper.findByPlateNumberAndParkCode(plateNumber, parkCode);
            
            if (existingEntry != null) {
                // 存在记录，更新预进场时间
                logger.info("🔄 [更新预进场数据] plateNumber={}, oldTime={}, newTime={}", 
                        plateNumber, existingEntry.getPreEnterTime(), preEnterTime);
                
                int updateResult = baseMapper.updatePreEnterTime(existingEntry.getId(), preEnterTime);
                if (updateResult > 0) {
                    logger.info("✅ [更新成功] plateNumber={}, id={}", plateNumber, existingEntry.getId());
                    return true;
                } else {
                    logger.error("❌ [更新失败] plateNumber={}, id={}", plateNumber, existingEntry.getId());
                    return false;
                }
            } else {
                // 不存在记录，插入新记录
                logger.info("➕ [插入新预进场数据] plateNumber={}", plateNumber);
                TempCarPreEntry tempCarPreEntry = new TempCarPreEntry();
                tempCarPreEntry.setPlateNumber(plateNumber);
                tempCarPreEntry.setParkCode(parkCode);
                tempCarPreEntry.setParkName(parkName);
                tempCarPreEntry.setEnterChannelCode(enterChannelCode);
                tempCarPreEntry.setEnterChannelId(enterChannelId);
                tempCarPreEntry.setEnterVipType(enterVipType);
                tempCarPreEntry.setPreEnterTime(preEnterTime);
                tempCarPreEntry.setCreateTime(new Date());
                tempCarPreEntry.setUsed(0); // 初始为未使用
                tempCarPreEntry.setRemark("临时车预进场数据自动记录");
                boolean result = this.save(tempCarPreEntry);
                if (result) {
                    logger.info("✅ [插入成功] plateNumber={}, id={}", plateNumber, tempCarPreEntry.getId());
                } else {
                    logger.error("❌ [插入失败] plateNumber={}", plateNumber);
                }
                
                return result;
            }
            
        } catch (Exception e) {
            logger.error("❌ [保存临时车预进场数据异常] plateNumber={}, error={}", plateNumber, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String getLatestPreEnterTime(String plateNumber, String parkCode) {
        try {
            logger.info("🔍 [查询最近预进场时间] plateNumber={}, parkCode={}", plateNumber, parkCode);
            
            // 使用自定义Mapper方法查询未使用的记录
            TempCarPreEntry tempCarPreEntry = baseMapper.findUnusedByPlateNumberAndParkCode(plateNumber, parkCode);
            
            if (tempCarPreEntry != null) {
                String preEnterTime = tempCarPreEntry.getPreEnterTime();
                logger.info("✅ [找到预进场时间] plateNumber={}, preEnterTime={}, id={}", 
                        plateNumber, preEnterTime, tempCarPreEntry.getId());
                return preEnterTime;
            } else {
                logger.info("ℹ️ [未找到预进场时间] plateNumber={}, parkCode={}", plateNumber, parkCode);
                return null;
            }
            
        } catch (Exception e) {
            logger.error("❌ [查询预进场时间异常] plateNumber={}, parkCode={}, error={}", 
                    plateNumber, parkCode, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public boolean markAsUsed(String plateNumber, String parkCode, String preEnterTime) {
        try {
            logger.info("🔄 [标记为已使用] plateNumber={}, parkCode={}, preEnterTime={}", 
                    plateNumber, parkCode, preEnterTime);
            
            // 先查询记录获取ID
            TempCarPreEntry tempCarPreEntry = baseMapper.findUnusedByPlateNumberAndParkCode(plateNumber, parkCode);
            if (tempCarPreEntry != null && preEnterTime.equals(tempCarPreEntry.getPreEnterTime())) {
                // 使用Mapper方法标记为已使用
                int result = baseMapper.markAsUsed(tempCarPreEntry.getId());
                if (result > 0) {
                    logger.info("✅ [标记成功] plateNumber={}, id={}", plateNumber, tempCarPreEntry.getId());
                    return true;
                } else {
                    logger.warn("⚠️ [标记失败] plateNumber={}, id={}", plateNumber, tempCarPreEntry.getId());
                    return false;
                }
            } else {
                logger.warn("⚠️ [标记失败] plateNumber={}, 记录不存在或时间不匹配", plateNumber);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("❌ [标记为已使用异常] plateNumber={}, parkCode={}, error={}", 
                    plateNumber, parkCode, e.getMessage(), e);
            return false;
        }
    }
} 