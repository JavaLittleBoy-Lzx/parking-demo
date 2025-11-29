package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.parkingmanage.entity.Community;
import com.parkingmanage.entity.Room;
import com.parkingmanage.mapper.RoomMapper;
import com.parkingmanage.service.RoomService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 <p>
  服务实现类
 </p>

 @author MLH
 @since 2023-02-06
*/
@Service
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements RoomService {
    @Override
    public int duplicate(Room room) {

        return baseMapper.duplicate(room);
    }
    @Override
    public List<Room> queryCommunity(Wrapper<Room> wrapper) {

        return baseMapper.queryCommunity(wrapper);
    }
    @Override
    public List<Room> getDistinctCommunity() {
        return baseMapper.getDistinctCommunity();
    }
    @Override
    public List<Room> queryManage(String openid,String  province,String city,String district,String community)
    {
        return baseMapper.queryManage(openid, province,city,district,community);
    }
    @Override
    public List<Room> allManage(String  province,String city,String district,String community)
    {
        return baseMapper.allManage(province,city,district,community);
    }
    @Override
    public List<Room> provinceList()
    {
        return baseMapper.provinceList();
    }
    @Override
    public List<Room> cityList(String province)
    {
        return baseMapper.cityList(province);
    }
    @Override
    public List<Room> districtList(String province,String city)
    {
        return baseMapper.districtList(province,city);
    }
    @Override
    public List<Room> communityList(String province,String city,String district)
    {
        return baseMapper.communityList(province,city,district);
    }
    @Override
    public List<Room> buildingList(String province,String city,String district,String community)
    {
        return baseMapper.buildingList(province,city,district,community);
    }
    @Override
    public List<Room> unitsList(String province,String city,String district,String community,String building)
    {
        return baseMapper.unitsList(province,city,district,community,building);
    }
    @Override
    public  List<Room> floorList(String province,String city,String district,String community,String building,String units){
        return baseMapper.floorList(province,city,district,community,building,units);
    }
    @Override
    public  List<Room> distinctPage(String province,String city,String district,String community){
        return baseMapper.distinctPage(province,city,district,community);
    }
}
