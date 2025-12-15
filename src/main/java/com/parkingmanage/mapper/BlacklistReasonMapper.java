package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.entity.BlacklistReason;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 拉黑原因模板Mapper接口
 * @author system
 * @date 2025-01-31
 */
@Mapper
public interface BlacklistReasonMapper extends BaseMapper<BlacklistReason> {
    
    /**
     * 分页查询拉黑原因列表
     * @param page 分页对象
     * @param reasonText 原因内容（模糊查询）
     * @param reasonCategory 原因分类
     * @param parkName 车场名称
     * @param isEnabled 是否启用
     * @return 拉黑原因分页列表
     */
    Page<BlacklistReason> selectReasonPage(
            Page<BlacklistReason> page,
            @Param("reasonText") String reasonText,
            @Param("reasonCategory") String reasonCategory,
            @Param("parkName") String parkName,
            @Param("isEnabled") Boolean isEnabled
    );
    
    /**
     * 查询启用的拉黑原因列表（用于下拉选择）
     * @param reasonCategory 原因分类（可为null）
     * @param parkName 车场名称（可为null）
     * @return 拉黑原因列表
     */
    List<BlacklistReason> selectEnabledReasons(
            @Param("reasonCategory") String reasonCategory,
            @Param("parkName") String parkName
    );
    
    /**
     * 增加使用次数
     * @param id 原因ID
     */
    void incrementUsageCount(@Param("id") Long id);
}

