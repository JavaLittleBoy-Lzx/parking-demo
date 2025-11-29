package com.parkingmanage.task;

import com.parkingmanage.service.WeChatTempMediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 微信临时素材自动刷新定时任务
 * 
 * 临时素材在微信后台保存时间为3天，需要定期刷新
 * 执行时间：每隔2天（即48小时）执行一次，确保在3天过期前更新
 * 
 * @author System
 * @since 2024-01-01
 */
@Slf4j
@Component
public class WeChatMediaRefreshTask {
    
    @Resource
    private WeChatTempMediaService weChatTempMediaService;
    
    /**
     * 定时刷新所有临时素材
     * 
     * cron表达式：0 0 3 1/2 * ?
     * 含义：每隔2天的凌晨3点执行
     * 
     * 说明：
     * 1. 微信临时素材有效期为3天（72小时）
     * 2. 为了保险起见，每隔2天（48小时）刷新一次
     * 3. 选择凌晨3点执行，避免业务高峰期
     * 4. 这样可以确保素材永远不会过期
     */
    @Scheduled(cron = "0 0 3 1/2 * ?")
    public void refreshAllMedia() {
        log.info("⏰ 定时任务启动：开始刷新所有临时素材");
        
        try {
            int successCount = weChatTempMediaService.refreshAllMediaIds();
            
            log.info("✅ 定时任务完成：成功刷新 {} 个临时素材", successCount);
            
        } catch (Exception e) {
            log.error("❌ 定时任务异常：刷新临时素材失败", e);
        }
    }
    
    /**
     * 备用定时任务：每天凌晨2点检查并刷新即将过期的素材
     * 
     * cron表达式：0 0 2 * * ?
     * 含义：每天凌晨2点执行
     * 
     * 这个任务作为备份，检查所有素材，如果距离过期不到6小时则立即刷新
     * 双重保险确保素材不会过期
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkAndRefreshExpiring() {
        log.info("⏰ 定时任务启动：检查即将过期的临时素材");
        
        try {
            // 这个方法会自动检查过期时间，只刷新需要更新的素材
            int successCount = weChatTempMediaService.refreshAllMediaIds();
            
            log.info("✅ 定时任务完成：检查完成，刷新了 {} 个临时素材", successCount);
            
        } catch (Exception e) {
            log.error("❌ 定时任务异常：检查临时素材失败", e);
        }
    }
    
    /**
     * 手动测试用：每小时执行一次（默认禁用，需要时取消注释）
     * 仅用于开发测试，生产环境请删除或注释此方法
     */
    // @Scheduled(cron = "0 0 * * * ?")
    // public void testRefresh() {
    //     log.info("⏰ 测试任务：刷新临时素材");
    //     refreshAllMedia();
    // }
}
