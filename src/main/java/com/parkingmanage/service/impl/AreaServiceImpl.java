package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.parkingmanage.entity.Area;
import com.parkingmanage.mapper.AreaMapper;
import com.parkingmanage.query.TransmitAreaQuery;
import com.parkingmanage.service.AreaService;
import com.parkingmanage.vo.AreaResult;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 <p>
  服务实现类
 </p>
 @author MLH
 * @since 2022-09-08
 */
@Service
public class AreaServiceImpl extends ServiceImpl<AreaMapper, Area> implements AreaService {
    @Override
    public Area getByOpenId(String openid) {
        return baseMapper.getByOpenId(openid);
    }
    @Override
    public List<AreaResult>  getAreaByOpenId(String openid) {
        return baseMapper.getAreaByOpenId(openid);
    }
    @Override
    public List<Area> getTransmitByOpenId(TransmitAreaQuery transmitAreaQuery) {
        return baseMapper.getTransmitByOpenId(transmitAreaQuery.getOpenid(),transmitAreaQuery.getProvince(),transmitAreaQuery.getCity(),transmitAreaQuery.getDistrict(),
                transmitAreaQuery.getCommunity());
    }
    @Override
    public Area getParkingInfo(String province,String city,String district,String community,String building){
        return baseMapper.getParkingInfo(province,city,district,community, building);}
    @Override
    public void deleteByOpenid(String openid){
         baseMapper.deleteByOpenid(openid);
    }
    @Override
    public void insertArea(String usercode, String username, String openid,  List<Integer> arrayId){
        baseMapper.insertArea(usercode,username,openid,arrayId);
    }
    @Override
    public void deleteArea(String province, String city, String district, String community, String usercode){
        baseMapper.deleteArea(province,city,district,community,usercode);
    }
}
