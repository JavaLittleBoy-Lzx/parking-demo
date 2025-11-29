package com.parkingmanage.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.parkingmanage.entity.QrVisitorUsage;
import com.parkingmanage.mapper.QrVisitorUsageMapper;
import com.parkingmanage.mapper.VisitorTokenMapper;
import com.parkingmanage.service.QrVisitorUsageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 访客记录清理定时任务
 * 
 * 包含4个定时任务：
 * 1. 每5分钟清理过期Token
 * 2. 每天零点重置每日扫码次数
 * 3. 每小时标记过期访客记录
 * 4. 每天凌晨1点删除30天前旧记录
 * 
 * @author System
 * @since 2025-11-23
 */
@Component
public class VisitorCleanupTask {
    
    private static final Logger logger = LoggerFactory.getLogger(VisitorCleanupTask.class);
    
    @Resource
    private QrVisitorUsageService qrVisitorUsageService;
    
    @Resource
    private QrVisitorUsageMapper qrVisitorUsageMapper;
    
    @Resource
    private VisitorTokenMapper visitorTokenMapper;
    
    /**
     * 每5分钟清理一次过期Token
     * 
     * 说明：
     * - Token有效期5分钟
     * - 每5分钟清理一次即可，不需要更频繁
     * - 清理过期的Token，释放数据库空间
     */
//    @Scheduled(cron = "0 */5 * * * ?")
    public void cleanExpiredTokens() {
        logger.info("🧹 [定时任务] 开始清理过期Token");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            int deleted = visitorTokenMapper.deleteExpired(now);
            
            if (deleted > 0) {
                logger.info("✅ [定时任务] 清理完成，共删除 {} 条过期Token", deleted);
            } else {
                logger.debug("✅ [定时任务] 没有过期Token需要清理");
            }
            
        } catch (Exception e) {
            logger.error("❌ [定时任务] 清理过期Token失败:", e);
        }
    }
    
    /**
     * 每天零点重置scan_count（每日使用次数）
     * 
     * 说明：
     * - 每个访客每天最多扫码3次
     * - 零点重置后，新的一天重新计数
     * - 只重置活跃状态的记录
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyScanCount() {
        logger.info("🔄 [定时任务] 开始重置每日扫码次数");
        
        try {
            int updated = qrVisitorUsageMapper.resetDailyScanCount();
            logger.info("✅ [定时任务] 重置完成，共更新 {} 条记录", updated);
            
        } catch (Exception e) {
            logger.error("❌ [定时任务] 重置每日扫码次数失败:", e);
        }
    }
    
    /**
     * 每小时标记过期的访客记录
     * 
     * 说明：
     * - 访客身份有效期24小时
     * - 每小时检查一次，标记过期记录
     * - 将status从'active'改为'expired'
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void markExpiredRecords() {
        logger.info("⏰ [定时任务] 开始标记过期访客记录");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 查询过期记录
            List<QrVisitorUsage> expiredList = qrVisitorUsageService.list(
                new QueryWrapper<QrVisitorUsage>()
                    .eq("status", "active")
                    .lt("expires_at", now)
            );
            
            if (expiredList.isEmpty()) {
                logger.debug("✅ [定时任务] 没有过期记录需要标记");
                return;
            }
            
            // 更新状态为已过期
            for (QrVisitorUsage usage : expiredList) {
                usage.setStatus("expired");
                qrVisitorUsageService.updateById(usage);
            }
            
            logger.info("✅ [定时任务] 标记完成，共处理 {} 条过期记录", expiredList.size());
            
        } catch (Exception e) {
            logger.error("❌ [定时任务] 标记过期记录失败:", e);
        }
    }
    
    /**
     * 每天凌晨1点删除30天前的过期记录
     * 
     * 说明：
     * - 只删除已过期的记录（status='expired'）
     * - 保留30天内的记录用于统计分析
     * - 避免数据库膨胀
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void deleteOldRecords() {
        logger.info("🗑️ [定时任务] 开始删除旧记录（30天前）");
        
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            int deleted = qrVisitorUsageMapper.deleteExpired(thirtyDaysAgo);
            
            if (deleted > 0) {
                logger.info("✅ [定时任务] 删除完成，共删除 {} 条旧记录", deleted);
            } else {
                logger.debug("✅ [定时任务] 没有旧记录需要删除");
            }
            
        } catch (Exception e) {
            logger.error("❌ [定时任务] 删除旧记录失败:", e);
        }
    }
}
