package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.parkingmanage.entity.Community;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 <p>
  Mapper 接口
 </p>

 @author yuli
 @since 2022-07-11
*/
public interface CommunityMapper extends BaseMapper<Community> {
    int duplicate(Community community);
    List<Community> queryCommunity(@Param(Constants.WRAPPER) Wrapper<Community> wrapper);
    List<Community> getDistinctCommunity();
    List<Community> queryManage(String openid,String  province,String city,String district,String community);
    List<Community> allManage(String  province,String city,String district,String community);
    List<Community> provinceList();
    List<Community> cityList(String province);
    List<Community> districtList(String province,String city);
    List<Community> communityList(String province,String city,String district);
    List<Community> buildingList(String province,String city,String district,String community);
    List<Community> unitsList(String province,String city,String district,String community,String building);
    List<Community> floorList(String province,String city,String district,String community,String building,String units );
    List<Community> distinctPage(String province,String city,String district,String community);
    List<Community> getCommunityInfo(Community community);
    List<Community> getBuilding(String province,String city,String district,String community);
    List<Community> getOnlyCommunity();
    List<Community> getOnlyBuilding(String province,String city,String district,String community);
    List<Community> getOnlyUnits(String province,String city,String district,String community,String building);
    List<Community> getOnlyFloor(String province,String city,String district,String community,String building,String units);
    List<Community> getOnlyRoomNumber(String province,String city,String district,String community,String building,String units,String floor);

    String findIsAuditByCommunityName(String community);

    Community butlerCommunityAuditTime(String butlerCommunity);

    List<Community> getCommunityName();

    List<Community> duplicatePage(String community);

    Community findProvinceByCommunityName(String community, String building, String floor, String units, String room);
}
