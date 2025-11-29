package com.parkingmanage.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.parkingmanage.entity.Community;
import com.parkingmanage.entity.Ownerinfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author MLH
 * @since 2022-08-20
 */
public interface OwnerinfoService extends IService<Ownerinfo> {
    List<Ownerinfo> queryOwner(Wrapper<Ownerinfo> wrapper);
    List<Ownerinfo> queryListOwner(String ownername, String community, String ownerphone, String plates);
    int duplicate(Ownerinfo ownerinfo);
    List<String> myCommunity(String userphone);
    List<Ownerinfo> myRooms(String community,String building,String units,
                            String floor,String roomnumber,String userphone);
    List<Ownerinfo> listByPhone(String  userphone);

    int updateByIdNew(Ownerinfo ownerinfo);

    List<Ownerinfo> phoneNumberOwnerInfo(String phoneNumber);

    int OwnerInfoByPhone(String ownerphone);

    String getManagerNicknameByCommunityName(String community);
}
