package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.parkingmanage.entity.Room;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 <p>
  Mapper 接口
 </p>
 @author MLH
 @since 2023-02-06
*/
public interface RoomMapper extends BaseMapper<Room> {
    int duplicate(Room room);
    List<Room> queryCommunity(@Param(Constants.WRAPPER) Wrapper<Room> wrapper);
    List<Room> getDistinctCommunity();
    List<Room> queryManage(String openid,String  province,String city,String district,String community);

    List<Room> allManage(String  province,String city,String district,String community);
    List<Room> provinceList();
    List<Room> cityList(String province);
    List<Room> districtList(String province,String city);

    List<Room> communityList(String province,String city,String district);

    List<Room> buildingList(String province,String city,String district,String community);
    List<Room> unitsList(String province,String city,String district,String community,String building);
    List<Room> floorList(String province,String city,String district,String community,String building,String units );
    List<Room> distinctPage(String province,String city,String district,String community);
}
