package com.parkingmanage.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.parkingmanage.entity.Community;
import com.parkingmanage.entity.Room;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 <p>
  服务类
 </p>

 @author MLH
 @since 2023-02-06
*/
public interface RoomService extends IService<Room> {
    int duplicate(Room room);
    List<Room> queryCommunity(Wrapper<Room> wrapper);
    List<Room> getDistinctCommunity();
    List<Room> queryManage(String openid,String  province,String city,String district,String community);
    List<Room> allManage(String  province,String city,String district,String community);
    List<Room> provinceList();
    List<Room> cityList(String province);
    List<Room> districtList(String province,String city);
    List<Room> communityList(String province,String city,String district);
    List<Room> buildingList(String province,String city,String district,String community);
    List<Room> unitsList(String province,String city,String district,String community,String building);
    List<Room> floorList(String province,String city,String district,String community,String building,String units);
    List<Room> distinctPage(String province,String city,String district,String community);

}
