package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.ViolationType;
import java.util.List;

/**
 * 违规类型配置Service接口
 * @author system
 * @date 2025-01-31
 */
public interface ViolationTypeService extends IService<ViolationType> {
    
    /**
     * 分页查询违规类型列表
     * @param page 页码
     * @param size 每页大小
     * @param typeName 类型名称（模糊查询）
     * @param parkName 车场名称
     * @param severityLevel 严重程度
     * @param isEnabled 是否启用
     * @return 分页结果
     */
    Page<ViolationType> getTypePage(Integer page, Integer size, 
                                   String typeName, String parkName, String severityLevel, Boolean isEnabled);
    
    /**
     * 查询启用的违规类型列表（用于下拉选择）
     * @param parkName 车场名称
     * @return 类型列表
     */
    List<ViolationType> getEnabledTypes(String parkName);
    
    /**
     * 新增违规类型
     * @param type 类型信息
     * @return 是否成功
     */
    boolean addType(ViolationType type);
    
    /**
     * 更新违规类型
     * @param type 类型信息
     * @return 是否成功
     */
    boolean updateType(ViolationType type);
    
    /**
     * 删除违规类型
     * @param id 类型ID
     * @return 是否成功
     */
    boolean deleteType(Long id);
    
    /**
     * 切换启用状态
     * @param id 类型ID
     * @param isEnabled 是否启用
     * @return 是否成功
     */
    boolean toggleEnabled(Long id, Boolean isEnabled);
}

