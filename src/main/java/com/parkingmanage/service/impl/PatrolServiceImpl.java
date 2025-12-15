package com.parkingmanage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parkingmanage.entity.Butler;
import com.parkingmanage.entity.Ownerinfo;
import com.parkingmanage.entity.Patrol;
import com.parkingmanage.mapper.PatrolMapper;
import com.parkingmanage.service.ButlerService;
import com.parkingmanage.service.PatrolService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    public Patrol getPatrolByPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<Patrol> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Patrol::getPhone, phone.trim());
        return patrolService.getOne(queryWrapper);
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
    
    @Override
    public List<Patrol> getOnDutyPatrolsByCommunity(String community) {
        log.info("📋 [查询值班巡检员] 开始查询 - 小区: {}", community);
        
        LambdaQueryWrapper<Patrol> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Patrol::getCommunity, community)
               .eq(Patrol::getNotificationEnabled, 1) // 只查询值班中的
               .eq(Patrol::getStatus, "已确定") // 只查询已确认的
               .isNotNull(Patrol::getOpenid); // 必须有openid
        
        List<Patrol> patrols = this.list(wrapper);
        log.info("📋 [查询值班巡检员] 查询完成 - 小区: {}, 值班人数: {}", community, patrols.size());
        
        if (patrols.isEmpty()) {
            log.warn("⚠️ [查询值班巡检员] 小区 {} 当前无值班巡检员！", community);
        }
        
        return patrols;
    }
}
