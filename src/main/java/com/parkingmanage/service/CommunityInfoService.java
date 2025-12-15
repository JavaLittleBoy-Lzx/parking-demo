package com.parkingmanage.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.CommunityInfo;

/**
 * 小区基本信息服务接口
 * 
 * @author system
 * @since 2024-12-01
 */
public interface CommunityInfoService extends IService<CommunityInfo> {
    
    /**
     * 根据小区名称查询小区信息
     * 
     * @param communityName 小区名称
     * @return 小区信息
     */
    CommunityInfo getByCommunityName(String communityName);
    
    /**
     * 根据省市区小区查询小区信息
     * 
     * @param province 省份
     * @param city 城市
     * @param district 区县
     * @param community 小区名称
     * @return 小区信息
     */
    CommunityInfo getByLocation(String province, String city, String district, String community);
}
