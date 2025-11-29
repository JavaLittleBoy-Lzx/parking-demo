package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkingmanage.entity.Butler;
import com.parkingmanage.entity.Ownerinfo;
import com.parkingmanage.entity.Patrol;
import com.parkingmanage.mapper.PatrolMapper;
import com.parkingmanage.service.ButlerService;
import com.parkingmanage.service.PatrolService;
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
public class PatrolServiceImpl extends ServiceImpl<PatrolMapper, Patrol> implements PatrolService {
    @Resource
    private PatrolService patrolService;
    @Override
    public int duplicate(Patrol patrol) {

        return baseMapper.duplicate(patrol);
    }
    @Override
    public Patrol getPatrolByOpenId(String openid) {
        return baseMapper.getPatrolByOpenId(openid);
    }
    @Override
    public List<Patrol> queryListPatrol(String username, String community){
        LambdaQueryWrapper<Patrol> queryWrapper = new LambdaQueryWrapper();

        if (StringUtils.hasLength(username)) {
            queryWrapper.like(Patrol::getUsername, username);
        }
        if (StringUtils.hasLength(community)) {
            queryWrapper.like(Patrol::getCommunity, community);
        }
        List<Patrol> patrols = patrolService.list(queryWrapper);

        return patrols;
    }
}
