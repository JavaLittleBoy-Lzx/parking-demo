package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkingmanage.entity.Community;
import com.parkingmanage.entity.Member;
import com.parkingmanage.entity.YardInfo;
import com.parkingmanage.mapper.CommunityMapper;
import com.parkingmanage.service.CommunityService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author yuli
 * @since 2022-07-11
 */
@Service
public class CommunityServiceImpl extends ServiceImpl<CommunityMapper, Community> implements CommunityService {
    @Autowired
    private CommunityService communityService;

    @Override
    public int duplicate(Community community) {
        return baseMapper.duplicate(community);
    }

    @Override
    public List<Community> queryCommunity(Wrapper<Community> wrapper) {
        return baseMapper.queryCommunity(wrapper);
    }

    @Override
    public List<Community> getDistinctCommunity() {
        return baseMapper.getDistinctCommunity();
    }

    @Override
    public List<Community> queryManage(String openid, String province, String city, String district, String community) {
        return baseMapper.queryManage(openid, province, city, district, community);
    }

    @Override
    public List<Community> allManage(String province, String city, String district, String community) {
        return baseMapper.allManage(province, city, district, community);
    }

    @Override
    public List<Community> provinceList() {
        return baseMapper.provinceList();
    }

    @Override
    public List<Community> cityList(String province) {
        return baseMapper.cityList(province);
    }

    @Override
    public List<Community> districtList(String province, String city) {
        return baseMapper.districtList(province, city);
    }

    @Override
    public List<Community> communityList(String province, String city, String district) {
        return baseMapper.communityList(province, city, district);
    }

    @Override
    public List<Community> buildingList(String province, String city, String district, String community) {
        return baseMapper.buildingList(province, city, district, community);
    }

    @Override
    public List<Community> unitsList(String province, String city, String district, String community, String building) {
        return baseMapper.unitsList(province, city, district, community, building);
    }

    @Override
    public List<Community> floorList(String province, String city, String district, String community, String building, String units) {
        return baseMapper.floorList(province, city, district, community, building, units);
    }

    @Override
    public List<Community> distinctPage(String province, String city, String district, String community) {
        return baseMapper.distinctPage(province, city, district, community);
    }

    public List<Community> getCommunityInfo(Community community) {
        return baseMapper.getCommunityInfo(community);
    }

    public List<Community> getBuilding(String province, String city, String district, String community) {
        return baseMapper.getBuilding(province, city, district, community);
    }

    @Override
    public String findIsAuditByCommunityName(String community) {
        return baseMapper.findIsAuditByCommunityName(community);
    }

    @Override
    public Community butlerCommunityAuditTime(String butlerCommunity) {
        return baseMapper.butlerCommunityAuditTime(butlerCommunity);
    }

    @Override
    public List<Community> getCommunityName() {
        return baseMapper.getCommunityName();
    }

    @Override
    public List<Community> duplicatePage(String community) {
        LambdaQueryWrapper<Community> queryWrapper = new LambdaQueryWrapper();
        if (StringUtils.hasLength(community)) {
            queryWrapper.like(Community::getCommunity, community);
        }
        List<Community> communityList = communityService.list(queryWrapper);
        return communityList;
    }

    @Override
    public Community findProvinceByCommunityName(String community, String building, String floor, String units, String room) {
        return baseMapper.findProvinceByCommunityName(community, building, floor, units, room);
    }


    public List<Community> getOnlyCommunity() {
        return baseMapper.getOnlyCommunity();
    }

    public List<Community> getOnlyBuilding(String province, String city, String district, String community) {
        return baseMapper.getOnlyBuilding(province, city, district, community);
    }

    public List<Community> getOnlyUnits(String province, String city, String district, String community, String building) {
        return baseMapper.getOnlyUnits(province, city, district, community, building);
    }

    public List<Community> getOnlyFloor(String province, String city, String district, String community, String building, String units) {
        return baseMapper.getOnlyFloor(province, city, district, community, building, units);
    }

    public List<Community> getOnlyRoomNumber(String province, String city, String district, String community, String building, String units, String floor) {
        return baseMapper.getOnlyRoomNumber(province, city, district, community, building, units, floor);
    }

}
