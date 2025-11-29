package com.parkingmanage.mapper;

import com.parkingmanage.entity.Butler;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parkingmanage.entity.Community;

import java.util.List;

/**
 <p>
  Mapper 接口
 </p>

 @author MLH
 @since 2023-02-11
*/
public interface ButlerMapper extends BaseMapper<Butler> {
    Butler getButlerByOpenId(String openid);
    Butler getButlerByPhone(String phone);
    Butler getButlerByName(String username);
    int duplicate(Butler butler);
    Butler getByUsercode(String province, String city, String district, String community, String usercode);

    List<Integer> getManageArea(String province, String city, String district, String community, String usercode);

    String getButlerByCommunity(String community);
    
    /**
     * 获取某个社区的所有管家列表
     * @param community 社区名称
     * @return 管家列表
     */
    List<Butler> getAllButlersByCommunity(String community);
}
