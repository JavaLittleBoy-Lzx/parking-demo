package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.QrVisitorUsage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 车场二维码访客使用记录 Mapper接口
 * @author System
 * @since 2025-11-23
 */
@Mapper
public interface QrVisitorUsageMapper extends BaseMapper<QrVisitorUsage> {
    
    /**
     * 重置所有活跃记录的今日扫码次数（定时任务：每天零点）
     * @return 更新的记录数
     */
    @Update("UPDATE qr_visitor_usage SET scan_count = 0 WHERE status = 'active'")
    int resetDailyScanCount();
    
    /**
     * 删除已过期的记录（定时任务：清理旧数据）
     * @param date 过期日期（如30天前）
     * @return 删除的记录数
     */
    @Delete("DELETE FROM qr_visitor_usage WHERE status = 'expired' AND updated_at < #{date}")
    int deleteExpired(@Param("date") LocalDateTime date);
    
    /**
     * 标记已过期的记录（定时任务：每小时）
     * @param now 当前时间
     * @return 更新的记录数
     */
    @Update("UPDATE qr_visitor_usage SET status = 'expired' WHERE status = 'active' AND expires_at < #{now}")
    int markExpired(@Param("now") LocalDateTime now);
}
