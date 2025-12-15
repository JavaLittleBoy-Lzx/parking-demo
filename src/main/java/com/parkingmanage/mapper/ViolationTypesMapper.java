package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.ViolationTypes;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 违规类型配置表 Mapper 接口
 * </p>
 *
 * @author MLH
 * @since 2025-01-31
 */
public interface ViolationTypesMapper extends BaseMapper<ViolationTypes> {

    /**
     * 获取分类的违规类型列表
     */
    Map<String, List<ViolationTypes>> selectViolationTypesByCategory();

    /**
     * 更新违规类型使用次数
     */
    int updateUsageCount(@Param("value") String value);

    /**
     * 检查违规类型值是否已存在
     */
    int checkValueExists(@Param("value") String value, @Param("excludeId") Long excludeId);

    /**
     * 获取启用的违规类型列表
     */
    List<ViolationTypes> selectActiveTypes();
}
