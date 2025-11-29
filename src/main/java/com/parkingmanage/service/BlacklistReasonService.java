package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.BlacklistReason;
import java.util.List;

/**
 * 拉黑原因模板Service接口
 * @author system
 * @date 2025-01-31
 */
public interface BlacklistReasonService extends IService<BlacklistReason> {
    
    /**
     * 分页查询拉黑原因列表
     * @param page 页码
     * @param size 每页大小
     * @param reasonText 原因内容（模糊查询）
     * @param reasonCategory 原因分类
     * @param parkName 车场名称
     * @param isEnabled 是否启用
     * @return 分页结果
     */
    Page<BlacklistReason> getReasonPage(Integer page, Integer size, 
                                       String reasonText, String reasonCategory, 
                                       String parkName, Boolean isEnabled);
    
    /**
     * 查询启用的拉黑原因列表（用于下拉选择）
     * @param reasonCategory 原因分类
     * @param parkName 车场名称
     * @return 原因列表
     */
    List<BlacklistReason> getEnabledReasons(String reasonCategory, String parkName);
    
    /**
     * 新增拉黑原因
     * @param reason 原因信息
     * @return 是否成功
     */
    boolean addReason(BlacklistReason reason);
    
    /**
     * 更新拉黑原因
     * @param reason 原因信息
     * @return 是否成功
     */
    boolean updateReason(BlacklistReason reason);
    
    /**
     * 删除拉黑原因
     * @param id 原因ID
     * @return 是否成功
     */
    boolean deleteReason(Long id);
    
    /**
     * 切换启用状态
     * @param id 原因ID
     * @param isEnabled 是否启用
     * @return 是否成功
     */
    boolean toggleEnabled(Long id, Boolean isEnabled);
    
    /**
     * 增加使用次数
     * @param id 原因ID
     */
    void incrementUsageCount(Long id);
}

