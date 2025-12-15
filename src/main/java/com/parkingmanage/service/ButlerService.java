package com.parkingmanage.service;

import com.parkingmanage.entity.Butler;
import com.baomidou.mybatisplus.extension.service.IService;
import com.parkingmanage.entity.Community;
import com.parkingmanage.entity.Member;
import com.parkingmanage.entity.Ownerinfo;

import java.util.List;

/**
 <p>
  服务类
 </p>

 @author Lzx
 @since 2023-02-11
*/
public interface ButlerService extends IService<Butler> {
    Butler getButlerByOpenId(String openid);
    Butler getButlerByPhone(String phone);
    Butler getButlerByName(String username);
    int duplicate(Butler butler);
    List<Butler> queryListButler(String username, String community);
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
