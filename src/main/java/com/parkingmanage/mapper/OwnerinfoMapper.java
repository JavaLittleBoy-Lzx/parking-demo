package com.parkingmanage.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.parkingmanage.entity.Ownerinfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author MLH
 * @since 2022-08-20
 */
public interface OwnerinfoMapper extends BaseMapper<Ownerinfo> {
    int duplicate(Ownerinfo ownerinfo);
    List<Ownerinfo> queryOwner(@Param(Constants.WRAPPER) Wrapper<Ownerinfo> wrapper);
    List<String> myCommunity(String userphone);
    List<Ownerinfo> myRooms(String community,String building,String units,String floor,
                            String roomnumber,String userphone);
    List<Ownerinfo> listByPhone(String  userphone);

    int updateByIdNew(Ownerinfo ownerinfo);

    List<Ownerinfo> phoneNumberOwnerInfo(String phoneNumber);

    int OwnerInfoByPhone(String ownerphone);

    String getManagerNicknameByCommunityName(String community);
}
