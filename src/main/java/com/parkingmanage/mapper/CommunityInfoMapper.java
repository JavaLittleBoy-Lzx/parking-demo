package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.CommunityInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 小区基本信息 Mapper 接口
 * 
 * @author system
 * @since 2024-12-01
 */
@Mapper
public interface CommunityInfoMapper extends BaseMapper<CommunityInfo> {
    
}
