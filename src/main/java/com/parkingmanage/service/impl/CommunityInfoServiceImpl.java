package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.CommunityInfo;
import com.parkingmanage.mapper.CommunityInfoMapper;
import com.parkingmanage.service.CommunityInfoService;
import org.springframework.stereotype.Service;

/**
 * 小区基本信息服务实现类
 * 
 * @author system
 * @since 2024-12-01
 */
@Service
public class CommunityInfoServiceImpl extends ServiceImpl<CommunityInfoMapper, CommunityInfo> 
        implements CommunityInfoService {

    @Override
    public CommunityInfo getByCommunityName(String communityName) {
        QueryWrapper<CommunityInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("community", communityName);
        wrapper.last("LIMIT 1");
        return this.getOne(wrapper);
    }

    @Override
    public CommunityInfo getByLocation(String province, String city, String district, String community) {
        QueryWrapper<CommunityInfo> wrapper = new QueryWrapper<>();
        
        if (province != null && !province.isEmpty()) {
            wrapper.eq("province", province);
        }
        if (city != null && !city.isEmpty()) {
            wrapper.eq("city", city);
        }
        if (district != null && !district.isEmpty()) {
            wrapper.eq("district", district);
        }
        if (community != null && !community.isEmpty()) {
            wrapper.eq("community", community);
        }
        
        wrapper.last("LIMIT 1");
        return this.getOne(wrapper);
    }
}
