package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.entity.ViolationDescription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 违规描述模板Mapper接口
 * @author system
 * @date 2025-01-31
 */
@Mapper
public interface ViolationDescriptionMapper extends BaseMapper<ViolationDescription> {
    
    /**
     * 分页查询违规描述列表
     * @param page 分页对象
     * @param descriptionText 描述内容（模糊查询）
     * @param violationTypeCode 违规类型代码
     * @param parkName 车场名称
     * @param isEnabled 是否启用
     * @return 违规描述分页列表
     */
    Page<ViolationDescription> selectDescriptionPage(
            Page<ViolationDescription> page,
            @Param("descriptionText") String descriptionText,
            @Param("violationTypeCode") String violationTypeCode,
            @Param("parkName") String parkName,
            @Param("isEnabled") Boolean isEnabled
    );
    
    /**
     * 查询启用的违规描述列表（用于下拉选择）
     * @param violationTypeCode 违规类型代码（可为null）
     * @param parkName 车场名称（可为null）
     * @return 违规描述列表
     */
    List<ViolationDescription> selectEnabledDescriptions(
            @Param("violationTypeCode") String violationTypeCode,
            @Param("parkName") String parkName
    );
    
    /**
     * 增加使用次数
     * @param id 描述ID
     */
    void incrementUsageCount(@Param("id") Long id);
}

