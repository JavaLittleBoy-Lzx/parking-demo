package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkingmanage.entity.Butler;
import com.parkingmanage.entity.Community;
import com.parkingmanage.entity.Member;
import com.parkingmanage.entity.Ownerinfo;
import com.parkingmanage.mapper.ButlerMapper;
import com.parkingmanage.service.ButlerService;
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
 @since 2023-02-11
*/
@Service
public class ButlerServiceImpl extends ServiceImpl<ButlerMapper, Butler> implements ButlerService {
    @Resource
    private ButlerService butlerService;
    @Override
    public int duplicate(Butler butler) {
        return baseMapper.duplicate(butler);
    }
    @Override
    public Butler getButlerByOpenId(String openid) {
        return baseMapper.getButlerByOpenId(openid);
    }
    @Override
    public Butler getButlerByPhone(String phone) {
        return baseMapper.getButlerByPhone(phone);
    }
    @Override
    public Butler getButlerByName(String username) {
        return baseMapper.getButlerByName(username);
    }
    @Override
    public List<Butler> queryListButler(String username, String community){
        LambdaQueryWrapper<Butler> queryWrapper = new LambdaQueryWrapper();
        if (StringUtils.hasLength(username)) {
            queryWrapper.like(Butler::getUsername, username);
        }
        if (StringUtils.hasLength(community)) {
            queryWrapper.like(Butler::getCommunity, community);
        }
        List<Butler> butlers = butlerService.list(queryWrapper);
        return butlers;
    }
    @Override
    public Butler getByUsercode(String province, String city, String district, String community, String usercode){
        return baseMapper.getByUsercode( province,  city,  district,  community,  usercode);
    }
    public List<Integer> getManageArea(String province, String city, String district, String community, String usercode){
        return baseMapper.getManageArea(province,  city,  district,  community,  usercode);
    }

    @Override
    public String getButlerByCommunity(String community) {
        return baseMapper.getButlerByCommunity(community);
    }
    
    @Override
    public List<Butler> getAllButlersByCommunity(String community) {
        return baseMapper.getAllButlersByCommunity(community);
    }
}