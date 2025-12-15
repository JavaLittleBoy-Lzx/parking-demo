package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parkingmanage.entity.ViolationLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 违规位置配置Mapper接口
 * @author system
 * @date 2025-01-31
 */
@Mapper
public interface ViolationLocationMapper extends BaseMapper<ViolationLocation> {
    
    /**
     * 分页查询违规位置列表
     * @param page 分页对象
     * @param locationName 位置名称（模糊查询）
     * @param parkName 车场名称
     * @param isEnabled 是否启用
     * @return 违规位置分页列表
     */
    Page<ViolationLocation> selectLocationPage(
            Page<ViolationLocation> page,
            @Param("locationName") String locationName,
            @Param("parkName") String parkName,
            @Param("isEnabled") Boolean isEnabled
    );
    
    /**
     * 查询启用的违规位置列表（用于下拉选择）
     * @param parkName 车场名称（可为null，查询通用位置）
     * @return 违规位置列表
     */
    List<ViolationLocation> selectEnabledLocations(@Param("parkName") String parkName);
    
    /**
     * 根据位置名称和车场查询
     * @param locationName 位置名称
     * @param parkName 车场名称
     * @return 违规位置
     */
    ViolationLocation selectByNameAndPark(
            @Param("locationName") String locationName,
            @Param("parkName") String parkName
    );
}

