package com.parkingmanage.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.parkingmanage.entity.Community;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.Member;

import java.util.List;

/**
 <p>
 *  服务类
 * </p>
 *
 * @author yuli
 * @since 2022-07-11
 */
public interface CommunityService extends IService<Community> {
    int duplicate(Community community);
    List<Community> queryCommunity(Wrapper<Community> wrapper);
    List<Community> getDistinctCommunity();
    List<Community> queryManage(String openid,String  province,String city,String district,String community);
    List<Community> allManage(String  province,String city,String district,String community);
    List<Community> getCommunityInfo(Community community);
    List<Community> provinceList();
    List<Community> getOnlyCommunity();
    List<Community> getOnlyBuilding(String province,String city,String district,String community);
    List<Community> getOnlyUnits(String province,String city,String district,String community,String building);
    List<Community> getOnlyFloor(String province,String city,String district,String community,String building,String units);
    List<Community> getOnlyRoomNumber(String province,String city,String district,String community,String building,String units,String floor);
    List<Community> cityList(String province);
    List<Community> districtList(String province,String city);
    List<Community> communityList(String province,String city,String district);
    List<Community> buildingList(String province,String city,String district,String community);
    List<Community> unitsList(String province,String city,String district,String community,String building);
    List<Community> floorList(String province,String city,String district,String community,String building,String units);
    List<Community> distinctPage(String province,String city,String district,String community);
    List<Community> getBuilding(String province,String city,String district,String community);

    String findIsAuditByCommunityName(String community);

    Community butlerCommunityAuditTime(String butlerCommunity);

    List<Community> getCommunityName();

    List<Community> duplicatePage(String community);

    Community findProvinceByCommunityName(String community, String building, String floor, String units, String room);

}
