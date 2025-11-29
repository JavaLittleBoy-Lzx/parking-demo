package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.entity.ViolationType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 违规类型配置Mapper接口
 * @author system
 * @date 2025-01-31
 */
@Mapper
public interface ViolationTypeMapper extends BaseMapper<ViolationType> {
    
    /**
     * 分页查询违规类型列表
     * @param page 分页对象
     * @param typeName 类型名称（模糊查询）
     * @param parkName 车场名称
     * @param severityLevel 严重程度
     * @param isEnabled 是否启用
     * @return 违规类型分页列表
     */
    Page<ViolationType> selectTypePage(
            Page<ViolationType> page,
            @Param("typeName") String typeName,
            @Param("parkName") String parkName,
            @Param("severityLevel") String severityLevel,
            @Param("isEnabled") Boolean isEnabled
    );
    
    /**
     * 查询启用的违规类型列表（用于下拉选择）
     * @param parkName 车场名称（可为null，查询通用类型）
     * @return 违规类型列表
     */
    List<ViolationType> selectEnabledTypes(@Param("parkName") String parkName);
    
    /**
     * 根据类型代码和车场查询
     * @param typeCode 类型代码
     * @param parkName 车场名称
     * @return 违规类型
     */
    ViolationType selectByCodeAndPark(
            @Param("typeCode") String typeCode,
            @Param("parkName") String parkName
    );
}

