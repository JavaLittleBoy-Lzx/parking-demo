package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.ViolationDescription;
import java.util.List;

/**
 * 违规描述模板Service接口
 * @author system
 * @date 2025-01-31
 */
public interface ViolationDescriptionService extends IService<ViolationDescription> {
    
    /**
     * 分页查询违规描述列表
     * @param page 页码
     * @param size 每页大小
     * @param descriptionText 描述内容（模糊查询）
     * @param violationTypeCode 违规类型代码
     * @param parkName 车场名称
     * @param isEnabled 是否启用
     * @return 分页结果
     */
    Page<ViolationDescription> getDescriptionPage(Integer page, Integer size, 
                                                  String descriptionText, String violationTypeCode, 
                                                  String parkName, Boolean isEnabled);
    
    /**
     * 查询启用的违规描述列表（用于下拉选择）
     * @param violationTypeCode 违规类型代码
     * @param parkName 车场名称
     * @return 描述列表
     */
    List<ViolationDescription> getEnabledDescriptions(String violationTypeCode, String parkName);
    
    /**
     * 新增违规描述
     * @param description 描述信息
     * @return 是否成功
     */
    boolean addDescription(ViolationDescription description);
    
    /**
     * 更新违规描述
     * @param description 描述信息
     * @return 是否成功
     */
    boolean updateDescription(ViolationDescription description);
    
    /**
     * 删除违规描述
     * @param id 描述ID
     * @return 是否成功
     */
    boolean deleteDescription(Long id);
    
    /**
     * 切换启用状态
     * @param id 描述ID
     * @param isEnabled 是否启用
     * @return 是否成功
     */
    boolean toggleEnabled(Long id, Boolean isEnabled);
    
    /**
     * 增加使用次数
     * @param id 描述ID
     */
    void incrementUsageCount(Long id);
}

