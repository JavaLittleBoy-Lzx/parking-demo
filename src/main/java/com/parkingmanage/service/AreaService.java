package com.parkingmanage.service;

import com.parkingmanage.entity.Area;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.Member;
import com.parkingmanage.query.TransmitAreaQuery;
import com.parkingmanage.vo.AreaResult;

import java.util.List;

/**
 <p>
  服务类
 </p>

 @author MLH
 @since 2022-09-08
*/
public interface AreaService extends IService<Area> {
    Area getByOpenId(String openid);
    List<AreaResult> getAreaByOpenId(String openid);
    List<Area> getTransmitByOpenId(TransmitAreaQuery transmitAreaQuery);
    Area getParkingInfo( String province,String city,String district,String community,String building);
    void deleteByOpenid(String openid);

    void insertArea( String usercode, String username,String openid,List<Integer> arrayId);
    void deleteArea(String province, String city, String district, String community, String usercode);
}
