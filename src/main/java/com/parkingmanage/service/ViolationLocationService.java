package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.ViolationLocation;
import java.util.List;

/**
 * 违规位置配置Service接口
 * @author system
 * @date 2025-01-31
 */
public interface ViolationLocationService extends IService<ViolationLocation> {
    
    /**
     * 分页查询违规位置列表
     * @param page 页码
     * @param size 每页大小
     * @param locationName 位置名称（模糊查询）
     * @param parkName 车场名称
     * @param isEnabled 是否启用
     * @return 分页结果
     */
    Page<ViolationLocation> getLocationPage(Integer page, Integer size, 
                                            String locationName, String parkName, Boolean isEnabled);
    
    /**
     * 查询启用的违规位置列表（用于下拉选择）
     * @param parkName 车场名称
     * @return 位置列表
     */
    List<ViolationLocation> getEnabledLocations(String parkName);
    
    /**
     * 新增违规位置
     * @param location 位置信息
     * @return 是否成功
     */
    boolean addLocation(ViolationLocation location);
    
    /**
     * 更新违规位置
     * @param location 位置信息
     * @return 是否成功
     */
    boolean updateLocation(ViolationLocation location);
    
    /**
     * 删除违规位置
     * @param id 位置ID
     * @return 是否成功
     */
    boolean deleteLocation(Long id);
    
    /**
     * 切换启用状态
     * @param id 位置ID
     * @param isEnabled 是否启用
     * @return 是否成功
     */
    boolean toggleEnabled(Long id, Boolean isEnabled);
}

