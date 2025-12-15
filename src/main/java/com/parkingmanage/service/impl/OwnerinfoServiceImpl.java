package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkingmanage.entity.Community;
import com.parkingmanage.entity.Member;
import com.parkingmanage.entity.Ownerinfo;
import com.parkingmanage.mapper.OwnerinfoMapper;
import com.parkingmanage.service.MemberService;
import com.parkingmanage.service.OwnerinfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 <p>
  服务实现类
 </p>

 @author MLH
 @since 2022-08-20
*/
@Service
public class OwnerinfoServiceImpl extends ServiceImpl<OwnerinfoMapper, Ownerinfo> implements OwnerinfoService {
    @Resource
    private OwnerinfoService ownerinfoService;
    @Override
    public int duplicate(Ownerinfo ownerinfo) {

        return baseMapper.duplicate(ownerinfo);
    }
    @Override
    public List<String> myCommunity(String userphone){
        return baseMapper.myCommunity(userphone);
    };
    @Override
    public List<Ownerinfo> myRooms(String community,String building,String units,
                                   String floor,String roomnumber,String userphone){
        return baseMapper.myRooms(community,building,units,floor,roomnumber,userphone);
    };
    @Override
    public List<Ownerinfo> listByPhone(String  userphone){
        return baseMapper.listByPhone(userphone);
    }

    @Override
    public int updateByIdNew(Ownerinfo ownerinfo) {
        return baseMapper.updateByIdNew(ownerinfo);
    }

    @Override
    public List<Ownerinfo> phoneNumberOwnerInfo(String phoneNumber) {
        return baseMapper.phoneNumberOwnerInfo(phoneNumber);
    }

    @Override
    public int OwnerInfoByPhone(String ownerphone) {
        return baseMapper.OwnerInfoByPhone(ownerphone);
    }

    @Override
    public String getManagerNicknameByCommunityName(String community) {
        return baseMapper.getManagerNicknameByCommunityName(community);
    }

    ;
    @Override
    public List<Ownerinfo> queryOwner(Wrapper<Ownerinfo> wrapper) {
        return baseMapper.queryOwner(wrapper);
    }
    @Override
    public List<Ownerinfo> queryListOwner(String ownername, String community, String ownerphone, String plates){
        LambdaQueryWrapper<Ownerinfo> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasLength(ownername)) {
            queryWrapper.like(Ownerinfo::getOwnername, ownername);
        }
        if (StringUtils.hasLength(community)) {
            queryWrapper.like(Ownerinfo::getCommunity, community);
        }
        if (StringUtils.hasLength(ownerphone)) {
            queryWrapper.like(Ownerinfo::getOwnerphone, ownerphone);
        }
        if (StringUtils.hasLength(plates)) {
            queryWrapper.like(Ownerinfo::getPlates, plates);
        }
        List<Ownerinfo> ownerinfos = ownerinfoService.list(queryWrapper);

        return ownerinfos;
    }
}
