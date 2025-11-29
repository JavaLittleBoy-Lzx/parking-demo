package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.ViolationTypes;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 违规类型配置表 服务类
 * </p>
 *
 * @author MLH
 * @since 2025-01-31
 */
public interface ViolationTypesService extends IService<ViolationTypes> {

    /**
     * 获取分类的违规类型列表
     */
    Map<String, List<ViolationTypes>> getViolationTypesByCategory();

    /**
     * 创建违规类型
     */
    boolean createViolationType(ViolationTypes violationType);

    /**
     * 更新违规类型
     */
    boolean updateViolationType(ViolationTypes violationType);

    /**
     * 删除违规类型
     */
    boolean deleteViolationType(Long id);

    /**
     * 更新违规类型使用次数
     */
    boolean updateUsageCount(String value);

    /**
     * 检查违规类型值是否已存在
     */
    boolean checkValueExists(String value, Long excludeId);

    /**
     * 获取启用的违规类型列表
     */
    List<ViolationTypes> getActiveTypes();
}
