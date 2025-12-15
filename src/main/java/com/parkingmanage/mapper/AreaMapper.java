package com.parkingmanage.mapper;

import com.parkingmanage.entity.Area;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.Member;
import com.parkingmanage.vo.AreaResult;

import java.util.List;

/**
 <p>
  Mapper 接口
 </p>

 @author MLH
 @since 2022-09-08
*/
public interface AreaMapper extends BaseMapper<Area> {
    Area getByOpenId(String openid);
    List<AreaResult>  getAreaByOpenId(String openid);
    List<Area> getTransmitByOpenId(String openid, String province,String city,String district,String community);
    Area getParkingInfo( String province,String city,String district,String community,String building);
    void deleteByOpenid(String openid);
    void insertArea(String usercode, String username, String openid,  List<Integer> arrayId);
    void deleteArea(String province, String city, String district, String community, String usercode);
}
