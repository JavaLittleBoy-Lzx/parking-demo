package com.parkingmanage.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.parkingmanage.entity.Appointment;
import com.parkingmanage.mapper.AppointmentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约记录过期处理定时任务
 * 
 * 功能说明：
 * - 每10分钟检查待入场的预约记录
 * - 如果预约创建时间超过24小时且仍未入场，标记为已过期
 * - 支持黑名单检查等其他限制，但不限制待入场数量
 * 
 * @author System
 * @since 2025-12-05
 */
@Component
public class AppointmentExpirationTask {
    
    private static final Logger logger = LoggerFactory.getLogger(AppointmentExpirationTask.class);
    
    // 24小时过期时间（单位：小时）
    private static final int EXPIRATION_HOURS = 24;
    
    @Resource
    private AppointmentMapper appointmentMapper;
    
    /**
     * 每10分钟执行一次，检查并标记过期的预约记录
     * 
     * 执行时间：每10分钟执行一次（0分、10分、20分、30分、40分、50分）
     * 
     * 过期规则：
     * 1. 预约状态为"待入场"（venuestatus='待入场'）
     * 2. 审核状态为已通过（auditstatus='已通过' 或 '不审核'）
     * 3. 创建时间超过24小时（recorddate < now - 24小时）
     * 
     * 处理方式：
     * - 将 venuestatus 更新为 '已过期'
     * - 记录过期时间到 updatetime
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void checkAndExpireAppointments() {
        logger.info("⏰ [定时任务] 开始检查过期预约记录");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expirationTime = now.minusHours(EXPIRATION_HOURS);
            
            // 查询符合过期条件的预约记录
            // 条件：待入场 + 已通过审核 + 创建时间超过24小时
            QueryWrapper<Appointment> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("venuestatus", "待入场")
                       .in("auditstatus", "已通过", "不审核")
                       .lt("recorddate", expirationTime);
            
            List<Appointment> expiredList = appointmentMapper.selectList(queryWrapper);
            
            if (expiredList == null || expiredList.isEmpty()) {
                logger.info("✅ [定时任务] 没有过期的预约记录");
                return;
            }
            
            logger.info("📊 [定时任务] 找到 {} 条过期预约记录，开始处理...", expiredList.size());
            
            // 批量更新为已过期状态
            UpdateWrapper<Appointment> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("venuestatus", "待入场")
                        .in("auditstatus", "已通过", "不审核")
                        .lt("recorddate", expirationTime)
                        .set("venuestatus", "已过期")
                        .set("updatetime", now);
            
            int updatedCount = appointmentMapper.update(null, updateWrapper);
            
            logger.info("✅ [定时任务] 成功标记 {} 条预约为已过期", updatedCount);
            
            // 记录每条过期记录的详细信息（用于调试）
            if (logger.isDebugEnabled()) {
                for (Appointment appointment : expiredList) {
                    logger.debug("   - 预约ID: {}, 车牌: {}, 创建时间: {}, 业主手机: {}", 
                        appointment.getId(),
                        appointment.getPlatenumber(),
                        appointment.getRecorddate(),
                        appointment.getOwnerphone());
                }
            }
            
        } catch (Exception e) {
            logger.error("❌ [定时任务] 处理过期预约记录失败:", e);
        }
    }
    
    /**
     * 每天凌晨2点清理30天前的已过期记录
     * 
     * 说明：
     * - 只删除已过期的记录（venuestatus='已过期'）
     * - 保留30天内的记录用于统计分析
     * - 避免数据库膨胀
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanOldExpiredRecords() {
        logger.info("🗑️ [定时任务] 开始清理旧的已过期预约记录（30天前）");
        
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            
            // 删除30天前的已过期记录
            QueryWrapper<Appointment> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("venuestatus", "已过期")
                        .lt("auditdate", thirtyDaysAgo);
            
            int deletedCount = appointmentMapper.delete(deleteWrapper);
            
            if (deletedCount > 0) {
                logger.info("✅ [定时任务] 清理完成，共删除 {} 条旧的已过期记录", deletedCount);
            } else {
                logger.info("✅ [定时任务] 没有需要清理的旧记录");
            }
            
        } catch (Exception e) {
            logger.error("❌ [定时任务] 清理旧的已过期记录失败:", e);
        }
    }
}
