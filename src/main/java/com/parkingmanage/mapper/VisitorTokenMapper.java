package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.VisitorToken;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 访客Token Mapper接口
 * @author System
 * @since 2025-11-23
 */
@Mapper
public interface VisitorTokenMapper extends BaseMapper<VisitorToken> {
    
    /**
     * 删除过期Token（定时任务使用）
     * @param now 当前时间
     * @return 删除的记录数
     */
    @Delete("DELETE FROM visitor_token WHERE expire_time < #{now}")
    int deleteExpired(@Param("now") LocalDateTime now);
    
    /**
     * 删除已使用的Token（可选，用于清理）
     * @return 删除的记录数
     */
    @Delete("DELETE FROM visitor_token WHERE is_used = 1")
    int deleteUsed();
}
